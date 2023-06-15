/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBTestBase;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * See {@link deltix.qsrv.hf.tickdb.perfomancedrop} for problem description.
 *
 * <p> This test depends on external data. See {@link LocalTestBaseConfig}.
 *
 * <p>Original test that exposed the problem.
 * This test expects running TB instance on port 8056.
 *
 * <p>Note: this test not fails even if performance prop is present.
 */
public class Test_ThroughputDropAfterLiveMessage_Original extends TDBTestBase {
    private static final boolean EXTERNAL_DB = true;

    private static final int PORT = 8056;
    private static final int NUM_READERS = 2;
    public static final boolean USE_FIXED_STREAM_TYPE_CURSOR = true;

    private static final AtomicBoolean stopped = new AtomicBoolean(false);

    private static RecordClassDescriptor tradeDescriptor;

    public Test_ThroughputDropAfterLiveMessage_Original() {
        super(true, false, LocalTestBaseConfig.HOME);

    }

    @Override
    public void startup() throws Exception {
        if (!EXTERNAL_DB) {
            super.startup();
            //PrintStream out = new PrintStream(new FileOutputStream("output.txt"));
            //System.setOut(out);
        }
    }

    @Override
    public void shutdown() throws Exception {
        if (!EXTERNAL_DB) {
            super.shutdown();
        }
    }

    /*    @Override
    protected String getQuantServerHome(File folder) {
        return folder.getPath();
    }*/

    static class ReadThread extends Thread {
        private final DXTickStream stream;
        private volatile long count = 0;
        private volatile long prevCount = 0;
        private volatile long startTime;
        private volatile long finishTime = Long.MIN_VALUE;

        ReadThread(DXTickStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            startTime = System.nanoTime();

            SelectionOptions options = new SelectionOptions(true, false);
            options.restrictStreamType = USE_FIXED_STREAM_TYPE_CURSOR;
            TickCursor cursor = stream.createCursor(options);
            cursor.reset(Long.MIN_VALUE);
            cursor.subscribeToAllEntities();
            try {
                while (cursor.next()) {
                    ++count;

                    if (stopped.get())
                        break;
                }
            } finally {
                cursor.close();
                finishTime = System.nanoTime();
            }
        }

        double calcThroughput() {
            long endTime = finishTime == Long.MIN_VALUE ? System.nanoTime() : finishTime;
            double timePass = ((double) (endTime - startTime)) / 1000000000.0;
            long count = this.count;
            double throughput = ((double) (count - prevCount)) / timePass;
            this.prevCount = count;
            startTime = System.nanoTime();
            return throughput;
        }
    }

    static class MeasureTest extends Thread {

        private final ReadThread[] readers;

        MeasureTest(DXTickStream stream, int numberOfReaders) {
            readers = new ReadThread[numberOfReaders];
            for (int i = 0; i < readers.length; ++i)
                readers[i] = new ReadThread(stream);

            System.out.println("Created " + readers.length + " readers.");
        }

        @Override
        public void run() {
            for (ReadThread reader : readers)
                reader.start();

            try {
                while (!stopped.get()) {
                    Thread.sleep(1000);

                    double sumThroughput = 0.0;
                    for (ReadThread reader : readers)
                        sumThroughput += reader.calcThroughput();

                    System.out.println("Throughput = " + (long) sumThroughput + " msg/s.");
                }

                waitReaders();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        void waitReaders() throws InterruptedException {
            for (ReadThread reader : readers) {
                reader.join();
            }
        }
    }

    static class LiveReader extends Thread {
        private final DXTickStream stream;

        LiveReader(DXTickStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            TickCursor liveCursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, true));

            try {
                System.out.println("Opened live cursor for " + stream.getKey() + ".");
                while (liveCursor.next()) {
                    System.out.println("MSG: " + liveCursor.getMessage());

                    if (stopped.get())
                        break;
                }
            } finally {
                liveCursor.close();
            }
        }
    }

    private static DXTickStream createLiveTransStream(DXTickDB db) {
        DXTickStream liveTrans = db.getStream(LocalTestBaseConfig.TRANSIENT_KEY);
        if (liveTrans != null) {
            System.out.println(LocalTestBaseConfig.TRANSIENT_KEY + " stream exists. Removing...");
            liveTrans.delete();
            System.out.println("OK");
        }

        RecordClassDescriptor mmDescriptor = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        tradeDescriptor = StreamConfigurationHelper.mkTradeMessageDescriptor(
                mmDescriptor, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        StreamOptions options = StreamOptions.fixedType(StreamScope.TRANSIENT, LocalTestBaseConfig.TRANSIENT_KEY, LocalTestBaseConfig.TRANSIENT_KEY, 0, tradeDescriptor);
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.initialBufferSize = 1024 << 10;
        options.bufferOptions.maxBufferSize = 1024 << 10;
        options.bufferOptions.lossless = true;
        return db.createStream(options.name, options);
    }

    private static void sendMessages(DXTickStream stream, int num) throws InterruptedException {
        RawMessage msg = new RawMessage();
        msg.type = tradeDescriptor;
        msg.data = new byte[10];
        msg.setSymbol("AAA");

        final TickLoader loader = stream.createLoader(new LoadingOptions(true));
        while (!stopped.get()) {
            Thread.sleep(5000);

            msg.setNanoTime(System.nanoTime());
            System.out.println("Sent live message");
            loader.send(msg);
        }
    }

    @Test
    public void test () throws InterruptedException {

        DXTickDB db;
        if (EXTERNAL_DB) {
            String url = "dxtick://localhost:" + PORT;
            System.out.println("Connecting to " + url + "...");
            db = TickDBFactory.openFromUrl(url, false);
        } else {
            db = getTickDb();
        }

        final DXTickStream stream = db.getStream(LocalTestBaseConfig.STREAM_KEY);
        final DXTickStream liveTrans = createLiveTransStream(db);

        LiveReader liveReader = new LiveReader(liveTrans);
        liveReader.start();

        MeasureTest measureTest = new MeasureTest(stream, NUM_READERS);
        measureTest.start();



        Thread liveSender = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(20000);
                    sendMessages(liveTrans, 10);
                } catch (InterruptedException e) {
                }
            }
        };
        liveSender.start();

        Thread.sleep(TimeUnit.MINUTES.toMillis(2));
        stopped.set(true);

        measureTest.join();
        liveSender.join();
        Thread.sleep(10);
        if (liveReader.isAlive()) {
            liveReader.interrupt();
        }
    }


    public void init() throws IOException {

        QSHome.set(LocalTestBaseConfig.HOME);


        StartConfiguration config = StartConfiguration.create(true, false, false);


        config.port = PORT;

            // configure logging and memory monitoring
        //configure(config);


        //runner = new TomcatRunner(config);

    }
}