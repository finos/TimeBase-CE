/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.vsocket.TransportProperties;
import com.epam.deltix.util.vsocket.TransportType;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LatencyChecker {

    public static final boolean USE_DISRUPTOR_QUEUE = true;
    private TransportProperties transportProperties = new TransportProperties(TransportType.SOCKET_TCP, Home.getPath("temp/dxipc"));

    private static final int WARMUP = 100_000;
    private static int MEASUREMENTS = 200_000;
    private static int TOTAL = WARMUP + MEASUREMENTS;
    private static final int DEF_THROUGHPUT = 10000;
    private static final int ITERATIONS = 5;

    private RecordClassDescriptor tradeDescriptor;

    private final long[] result = new long[MEASUREMENTS];

    public DXTickStream getStream(DXTickDB db, StreamScope scope) {
        DXTickStream stream = db.getStream("message");
        if (stream != null)
            stream.delete();

        RecordClassDescriptor mcd = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        tradeDescriptor = StreamConfigurationHelper.mkTradeMessageDescriptor(
                mcd, null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        StreamOptions options = StreamOptions.fixedType(scope, "message", "message", 0, tradeDescriptor);
//        options.bufferOptions = new BufferOptions ();
//        options.bufferOptions.initialBufferSize = 1024  << 10;
//        options.bufferOptions.maxBufferSize = 1024 << 10;
//        options.bufferOptions.lossless = true;
        return db.createStream("message", options);
    }

    public void start(int throughput) throws Throwable {
        TDBRunner dbRunner = new TDBRunner(false, true, new TomcatServer());
        dbRunner.transportProperties = transportProperties;
        dbRunner.startup();

        DXTickStream stream = getStream(dbRunner.getTickDb(), StreamScope.DURABLE);

        LoadingOptions loadingOptions = new LoadingOptions(true);
        loadingOptions.channelPerformance = ChannelPerformance.LOW_LATENCY;
        TickLoader loader = stream.createLoader(loadingOptions);

        SelectionOptions selectionOptions = new SelectionOptions(true, true);
        selectionOptions.channelPerformance = ChannelPerformance.LOW_LATENCY;
        selectionOptions.allowLateOutOfOrder = true;

        TickCursor cursor = stream.createCursor(selectionOptions);
        cursor.subscribeToAllEntities();
        cursor.subscribeToAllTypes();
        cursor.reset(Long.MIN_VALUE);

        Thread.sleep(5000);

        MessageGenerator gn = new MessageGenerator(loader, throughput);
        Thread runner = new Thread(gn);
        runner.setDaemon(true);
        runner.start();

        StringBuilder sb = new StringBuilder();
        int iterationCount = 0;

        try {
            int messageCount = 0;

            //System.out.println ("     #Max  #Min   Avg [microseconds]");
            System.out.println("Round-trip latency test for " + MEASUREMENTS + " messages, throughput=" + throughput + " msg/sec");
            System.out.println ("Min\t10%\t50%\t75%\t90%\t99%\t99.9%\t99.99%\t99.999%\tMax\t[microseconds]");

            while (cursor.next()) {
                messageCount++;
                onMessage(cursor.getMessage(), messageCount);

                if (messageCount >= TOTAL) {
                    //HistogramData data = histogram.getHistogramData();

                    Arrays.sort(result);
                    sb.setLength(0);
                    sb
                            .append(result[0]).append("\t")
                            .append(result[(int) (MEASUREMENTS * 0.10)]).append("\t")
                            .append(result[(int) (MEASUREMENTS * 0.5)]).append("\t")
                            .append(result[(int) (MEASUREMENTS * 0.75)]).append("\t")
                            .append(result[(int) (MEASUREMENTS * 0.9)]).append("\t")
                            .append(result[(int) (MEASUREMENTS * 0.99)]).append("\t")
                            .append(result[(int) (MEASUREMENTS * 0.999)]).append("\t")
                            .append(result[(int) (MEASUREMENTS * 0.9999)]).append("\t")
                            .append(result[(int) (MEASUREMENTS * 0.99999)]).append("\t")
                            .append(result[MEASUREMENTS - 1])
                            .append("\n");

                    synchronized (System.out) {
                        for (int i = 0; i < sb.length(); i++)
                            System.out.write(sb.charAt(i));
                    }

                    Statistics stat = new Statistics(result, 0, MEASUREMENTS);

                    System.out.println("STDEV = " + stat.getStdDev());
                    System.out.println("MEAN = " + stat.getMean());

                    //Arrays.fill(result, 0);

                    //System.out.printf("%,9d %,5d %5.0f\n", result[result.length / 2], result[(int) (TOTAL * 0.9)], result[(int) (TOTAL * 0.9999)]); // nice output

                    //messageCount = 0;

                    if (iterationCount < ITERATIONS) {
                        iterationCount++;
                        messageCount = 0;
                    } else {
                        break;
                    }
                }
            }
        } finally {
            Util.close(loader);
            Util.close(cursor);
        }

        Thread.sleep(50); // Reader may need time to close
        dbRunner.shutdown();
    }



    public static void main(String[] args) throws Throwable {
        //System.setProperty("QuickExecutor.Sweeper.delay", String.valueOf(1000 * 60 * 60 * 24)); // 1 day

        if (args.length == 0) {
            System.out.println("Usage: <messages> <throughput> ");
            System.out.println("  <messages> is number of messages to send. Default is " + TOTAL);
            System.out.println("  <throughput> is number of messages per second. default is " + DEF_THROUGHPUT + ".");
        }

        int throughput = args.length > 0 ? Integer.parseInt(args[1]) : DEF_THROUGHPUT;
        MEASUREMENTS = args.length > 1 ? Integer.parseInt(args[2]) : TOTAL;

        if (USE_DISRUPTOR_QUEUE) {
            System.setProperty(TickStreamImpl.USE_DISRUPTOR_QUEUE_PROPERTY_NAME, "true");
        }

        new LatencyChecker().start(throughput);
    }

    public void onMessage(InstrumentMessage msg, int messageCount) {
        final long now = System.nanoTime();
        long latency = (now - msg.getNanoTime()) / 1000;

        if (messageCount > WARMUP) {
            //histogram.recordValue(latency);
//            minLatency = Math.min(minLatency, latency);
//            maxLatency = Math.max(maxLatency, latency);
            int count = messageCount - WARMUP - 1;
            result[count] = latency;
            //avgLatency = (avgLatency * (count - 1) + latency) / count;
        }
    }

    private class MessageGenerator implements Runnable {
        private static final long MAX_FEED_RATE_FOR_SPIN_LOCK = 10000;
        private final TickLoader loader;
        private int throughputMessagesPerSecond;
        private String[] table = new String[32];
        private int[] values = new int[32];

        public volatile boolean active = true;

        final RawMessage trade = new RawMessage();

        private MessageGenerator(TickLoader loader, int throughputMessagesPerSecond) {
            this.loader = loader;
            this.throughputMessagesPerSecond = throughputMessagesPerSecond;
            Random rnd = new Random(2012);

            assert (Integer.bitCount(table.length) == 1); // so that index can be (messageCount & (table.length-1))

            for (int i = 0; i < table.length; i++) {
                values[i] = Math.abs(rnd.nextInt()) % 10 + 48;
                table[i] = String.valueOf(values[i]);
            }

            trade.type = tradeDescriptor;
            trade.data = new byte[30];
            trade.setSymbol("DLTX");
        }

        @Override
        public void run() {
            final long intervalBetweenMessagesInNanos;
            if (throughputMessagesPerSecond > MAX_FEED_RATE_FOR_SPIN_LOCK)
                intervalBetweenMessagesInNanos = 0;
            else
                intervalBetweenMessagesInNanos = TimeUnit.SECONDS.toNanos(1) / throughputMessagesPerSecond;

            //final int indexMask = table.length-1;
            long messageCount = 0;

            while (messageCount <= TOTAL) {

                long nextNanoTime = (intervalBetweenMessagesInNanos != 0) ? System.nanoTime() + intervalBetweenMessagesInNanos : 0;
                while (messageCount <= TOTAL) {
                    if (intervalBetweenMessagesInNanos != 0) {
                        if (System.nanoTime() < nextNanoTime)
                            continue; // spin-wait
                    }

                    //msg.symbol = table[(int) (++messageCount & indexMask)];
                    //trade.originalTimestamp = System.nanoTime();
                    trade.setNanoTime(System.nanoTime());
                    //trade.timestamp = System.nanoTime();
                    loader.send (trade);

                    nextNanoTime += intervalBetweenMessagesInNanos;
                    messageCount++;
                }
            }

        }
    }


    public static class Statistics
    {
        long[] data;
        double length;
        int index;

        public Statistics(long[] data, int index, int length)
        {
            this.data = data;
            this.length = length;
            this.index = index;
        }

        double getMean()
        {
            double sum = 0.0;
            for (int i = 0; i < length; i++) {
                long a = data[i + index];
                sum += a;
            }
            return sum/length;
        }

        double getVariance()
        {
            double mean = getMean();
            double temp = 0;

            for (int i = 0; i < length; i++) {
                long a = data[i + index];
                temp += (mean - a) * (mean - a);
            }
            return temp/length;
        }

        double getStdDev() {
            return Math.sqrt(getVariance());
        }
    }


}