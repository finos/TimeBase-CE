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

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * See {@link deltix.qsrv.hf.tickdb.perfomancedrop} for problem description.
 *
 * <p> This test depends on external data. See {@link LocalTestBaseConfig}.
 *
 * <p>This is simplified version of {@link Test_ThroughputDropAfterLiveMessage_Original}.
 * Instead of working with external TimeBase, work with TB instance running in this JVM (locally).
 *
 * <p>Note: this test NOT fails even if performance prop is present.
 */
public class Test_ThroughputDropAfterLiveMessage_Macro extends Test_ThroughputDropAfterLiveMessage_Base {

    public static final boolean USE_FIXED_STREAM_TYPE_CURSOR = true;
    private static final int NUM_READERS = 2;

    private static final AtomicBoolean stopped = new AtomicBoolean(false);

    private static RecordClassDescriptor tradeDescriptor;

    private TickDBImpl db;
    public DataCacheOptions     options = new DataCacheOptions();


    public Test_ThroughputDropAfterLiveMessage_Macro() throws FileNotFoundException {

    }

    @Test
    public void test () throws InterruptedException {
        Thread.sleep(10000);

        DXTickDB db = getTickDb();

        final DXTickStream stream = db.getStream(LocalTestBaseConfig.STREAM_KEY);
        final DXTickStream liveTrans = createSecondStream(db);

        LiveReader liveReader = new LiveReader(liveTrans);
        liveReader.start();

        MeasureTest measureTest = new MeasureTest(stream, NUM_READERS);
        measureTest.start();



        Thread liveSender = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10000);
                    sendMessages(liveTrans, 10);
                } catch (InterruptedException e) {
                }
            }
        };
        liveSender.start();

        Thread.sleep(TimeUnit.SECONDS.toMillis(100));
        stopped.set(true);

        measureTest.join();
        liveSender.join();
        Thread.sleep(10);
        if (liveReader.isAlive()) {
            liveReader.interrupt();
        }

        fileOut.flush();
    }


    @Before
    public void startup() throws Throwable {
        super.startup();

        File folder = new File(LocalTestBaseConfig.HOME);
        db = new TickDBImpl(options, folder);
        db.open(false);
    }

    @After
    public void shutdown() throws Throwable {
        db.close();
        super.shutdown();
    }

    public TickDBImpl getTickDb() {
        return db;
    }

    /*    @Override
    protected String getQuantServerHome(File folder) {
        return folder.getPath();
    }*/

    static class ReadThread extends Thread {
        private final DXTickStream stream;
        private volatile long count;
        private volatile long startTime;
        private volatile long finishTime = Long.MIN_VALUE;

        ReadThread(DXTickStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            startTime = System.nanoTime();

            TickCursor cursor = getTickCursor(stream, USE_FIXED_STREAM_TYPE_CURSOR);
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
            double throughput = ((double) count) / timePass;
            count = 0;
            startTime = System.nanoTime();
            return throughput;
        }
    }

    class MeasureTest extends Thread {

        private final ReadThread[] readers;

        MeasureTest(DXTickStream stream, int numberOfReaders) {
            readers = new ReadThread[numberOfReaders];
            for (int i = 0; i < readers.length; ++i)
                readers[i] = new ReadThread(stream);

            printTimed("Created " + readers.length + " readers.");
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

                    printTimed("Throughput = " + (long) sumThroughput + " msg/s.");
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

    class LiveReader extends Thread {
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
                    printTimed("MSG: " + liveCursor.getMessage());

                    if (stopped.get())
                        break;
                }
            } finally {
                liveCursor.close();
            }
        }
    }

    private static DXTickStream createSecondStream(DXTickDB db) {
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
            System.out.println(getUptimeString() +": Sent live message");
            loader.send(msg);
        }
    }

    public void init() throws IOException {
        QSHome.set(LocalTestBaseConfig.HOME);
    }
}
