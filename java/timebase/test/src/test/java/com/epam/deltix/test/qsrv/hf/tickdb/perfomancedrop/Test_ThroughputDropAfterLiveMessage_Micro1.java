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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * See {@link deltix.qsrv.hf.tickdb.perfomancedrop} for problem description.
 *
 * <p> This test depends on external data. See {@link LocalTestBaseConfig}.
 *
 * <p>One of previous version of {@link Test_ThroughputDropAfterLiveMessage_Micro2} that still reproduces the problem.
 * This test is reproduces the problem without JVM arguments.
 * Adding "-XX:FreqInlineSize=3000 -XX:InlineSmallCode=5000" argument eliminates the problem.
 *
 * <p>This test fails if performance drop is detected.
 */
public class Test_ThroughputDropAfterLiveMessage_Micro1 extends Test_ThroughputDropAfterLiveMessage_Base {
    private static final int NUM_READERS = 2;

    private static final AtomicBoolean stopped = new AtomicBoolean(false);
    public static final int NUMBER_OF_LIVE_MESSAGES = 10;
    public static final int MEASUREMENT_DURATION_MS = 1_000;
    public static final int MEASUREMENT_ITERATIONS = 5;

    public static final boolean USE_FIXED_STREAM_TYPE_CURSOR = true;

    private static RecordClassDescriptor tradeDescriptor;

    private TickDBImpl db;
    public DataCacheOptions     options = new DataCacheOptions();

    public Test_ThroughputDropAfterLiveMessage_Micro1() throws FileNotFoundException {

    }

    @Test
    public void test() throws Exception {
        //Thread.sleep(10000);

        // Setup
        DXTickDB db = getTickDb();

        final DXTickStream mainStream = db.getStream(LocalTestBaseConfig.STREAM_KEY);
        final DXTickStream secondaryStream = createSecondStream(db);


        //TickCursor mainCursor = mainStream.select(Long.MIN_VALUE, new SelectionOptions(true, false), null, null);

        TickCursor liveCursor = secondaryStream.select(Long.MIN_VALUE, new SelectionOptions(true, true));
        TickLoader liveSender = secondaryStream.createLoader(new LoadingOptions(true));

        //RawMessage msg = createRawMessage();

        // Start readers
        List<MainStreamReader> readers = new ArrayList<>();
        for (int i = 0; i < NUM_READERS; i++) {
            MainStreamReader reader = new MainStreamReader(mainStream);
            mainExecutorService.submit(reader);
            readers.add(reader);
        }

        // Warmup
        Thread.sleep(3000);

        // Measure initial performance
        List<Integer> measurementsBefore = takeMeasurements(MEASUREMENT_ITERATIONS, MEASUREMENT_DURATION_MS, readers);
        Integer secondMinimumBefore = getMin(measurementsBefore, 2);
        printTimed("secondMinimumBefore: " + df.format(secondMinimumBefore));

        printTimed("Reading/sending live messages");
        // Start live message sender
        Thread liveSenderThread = new SecondaryMessageSender(liveSender);
        liveSenderThread.start();
        //sendMessages(NUMBER_OF_LIVE_MESSAGES, liveSender);

        // Receive live messages
        for (int i = 0; i < NUMBER_OF_LIVE_MESSAGES; i++) {
            liveCursor.next();
        }
        //liveSenderThread.interrupt();
        //liveSenderThread.join();
        printTimed("Done");

        Thread.sleep(1000);


        // Measure after live messages
        List<Integer> measurementsAfter = takeMeasurements(MEASUREMENT_ITERATIONS, MEASUREMENT_DURATION_MS, readers);
        Integer secondMaximumAfter = getMax(measurementsAfter, 2);
        printTimed("secondMaximumAfter: " + df.format(secondMaximumAfter));




        float performanceLossPercent = getLossPercent(secondMinimumBefore, secondMaximumAfter);
        if (performanceLossPercent > 5) {
            //printTimed("Performance LOSS: " + performanceLossPercent + "%");
            Assert.fail("Performance LOSS: " + performanceLossPercent + "%");
        } else {
            print("NO significant performance loss: " + performanceLossPercent + "%");
        }
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


    private DXTickStream createSecondStream(DXTickDB db) {
        DXTickStream liveTrans = db.getStream(LocalTestBaseConfig.TRANSIENT_KEY);
        if (liveTrans != null) {
            printTimed(LocalTestBaseConfig.TRANSIENT_KEY + " stream exists. Removing...");
            liveTrans.delete();
            printTimed("OK");
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

    private static void sendMessages(int num, TickLoader liveSender) throws InterruptedException {
        RawMessage msg = createRawMessage();

        int sentCount = 0;
        while (!stopped.get() && (sentCount < num)) {
            Thread.sleep(1000);

            msg.setNanoTime(System.nanoTime());
            //printTimed(getUptimeString() +": Sent live message");
            liveSender.send(msg);
            sentCount ++;
        }
    }

    private static RawMessage createRawMessage() {
        RawMessage msg = new RawMessage();
        msg.type = tradeDescriptor;
        msg.data = new byte[10];
        msg.setSymbol("AAA");
        return msg;
    }

    private final ExecutorService mainExecutorService = Executors.newCachedThreadPool();


    /**
     * @return sorted list of measurements
     */
    private List<Integer> takeMeasurements(int iterations, int durationMs, List<MainStreamReader> readers) {
        List<Integer> measurements = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            Integer measurementResult = makeMeasurement(durationMs, readers);
            measurements.add(measurementResult);
            print("Measurement: " + df.format(measurementResult));
        }
        return measurements;
    }

    private Integer makeMeasurement(int durationMs, List<MainStreamReader> readers) {
        // Note: taking results takes some time itself. For now we ignore that.
        long countBefore = 0;
        for (MainStreamReader reader : readers) {
            countBefore += reader.getTotalMessages();
        }

        long startTime = System.currentTimeMillis();
        try {
            // This time is not precise
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        long countAfter = 0;
        for (MainStreamReader reader : readers) {
            countAfter += reader.getTotalMessages();
        }
        long stopTime = System.currentTimeMillis();

        long totalTime = stopTime - startTime;

        return Math.round(((float) countAfter - countBefore) * 1000 / totalTime);
    }

    public void init() throws IOException {

        QSHome.set(LocalTestBaseConfig.HOME);

            // configure logging and memory monitoring
        //configure(config);


        //runner = new TomcatRunner(config);

    }

    static private class MainStreamReader implements Runnable {
        private final DXTickStream stream;
        private volatile long count = 0;
        private volatile long prevCount = 0;
        private volatile long startTime;
        private volatile long finishTime = Long.MIN_VALUE;

        MainStreamReader(DXTickStream stream) {
            this.stream = stream;
        }

        @Override
        public void run() {
            startTime = System.nanoTime();

            TickCursor cursor = getTickCursor(stream, USE_FIXED_STREAM_TYPE_CURSOR);
            try {
                while (cursor.next()) {
                    count ++;

                    if (stopped.get())
                        break;
                }
            } finally {
                cursor.close();
                finishTime = System.nanoTime();
            }
        }

        public double calcThroughput() {
            long endTime = finishTime == Long.MIN_VALUE ? System.nanoTime() : finishTime;
            double timePass = ((double) (endTime - startTime)) / 1000000000.0;
            long count = this.count;
            double throughput = ((double) (count - prevCount)) / timePass;
            this.prevCount = count;
            startTime = System.nanoTime();
            return throughput;
        }

        public long getTotalMessages() {
            return count;
        }
    }

    private static class SecondaryMessageSender extends Thread {
        private final TickLoader liveSender;

        public SecondaryMessageSender(TickLoader liveSender) {
            this.liveSender = liveSender;
        }

        @Override
        public void run() {
            try {
                //Thread.sleep(100);
                sendMessages(NUMBER_OF_LIVE_MESSAGES, liveSender);
            } catch (InterruptedException e) {
            }
        }
    }
}
