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
package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.lang.Util;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TestPerformance extends DefaultApplication {

    private RecordClassDescriptor   descriptor;

    public TestPerformance(String[] args) {
        super(args);
    }

    protected void increment(StringBuffer symbol, int index) {
        if (symbol.charAt(index) == (int)'Z') {
            increment(symbol, index - 1);
            symbol.setCharAt(index, 'A');
        }
        else
            symbol.setCharAt(index, (char) (symbol.charAt(index) + 1));
    }

    @Override
    protected void run() throws Throwable {
        String          tb = getArgValue ("-db", "dxtick://localhost:8011");
        int             throughput = getIntArgValue("-throughput", 200_000);
        int             size = getIntArgValue("-size", 100);
        int             df = getIntArgValue("-df", 0);
        int             symbols = getIntArgValue("-symbols", 1000);
        int             total = getIntArgValue("-total", 10_000_000);
        int             readers = getIntArgValue("-readers", 3);

        System.out.println("Running Loader performance test: ");
        System.out.println(String.format("    timebase: %s",tb));
        System.out.println(String.format("    write speed: %,d msg/sec", + throughput));
        System.out.println("    message size: " + size + " bytes");
        System.out.println("    Distribution Factor: " + df);
        System.out.println("    number of symbols: " + symbols);
        System.out.println(String.format("    number of messages: %,d", + total));
        System.out.println("    number of live readers: " + readers);


        descriptor = Messages.ERROR_MESSAGE_DESCRIPTOR;

        StringBuffer ch = new StringBuffer("AAAAAAAA");
        String[] names = new String[symbols];
        for (int i = 0; i < names.length; i++) {
            names[i] = ch.toString();
            increment(ch, 4);
        }

        String id = UUID.randomUUID().toString();
        try (DXTickDB db = TickDBFactory.createFromUrl(tb)) {
            db.open(false);
            DXTickStream stream = db.createStream(id, StreamOptions.fixedType(StreamScope.DURABLE, id, id, df, descriptor));

            Thread[] consumers = new Thread[readers];
            for (int i = 0; i < consumers.length; i++) {
                consumers[i] = new Thread(new MessageReader(stream, total));
                consumers[i].start();
            }

            try (TickLoader loader = stream.createLoader(new LoadingOptions(true))) {
                MessageGenerator generator = new MessageGenerator(loader, total, throughput, size, names);
                generator.run();
            }

            for (int i = 0; i < consumers.length; i++)
                consumers[i].join();

            stream.delete();
        }
    }

    private class MessageReader implements Runnable{

        private final DXTickStream stream;
        private final long messages;

        public MessageReader(DXTickStream stream, long messages) {
            this.stream = stream;
            this.messages = messages;
        }

        @Override
        public void run() {

            TickCursor cursor = null;
            try {
                SelectionOptions options = new SelectionOptions(true, true);
                cursor = stream.select(0, options);

                long                            t0 = System.currentTimeMillis ();
                long                            count = 0;

                while (cursor.next() && count < messages) {
                    count++;
//                    if (count % 10_000_000 == 0)
//                        System.out.printf("Read %,3d messages\n", count);
                }

                long                            t1 = System.currentTimeMillis ();
                double                          s = (t1 - t0) * 0.001;
                System.out.printf (
                        "Read %,d messages in %,.3fs; speed: %,.0f msg/s\n",
                        count,
                        s,
                        count / s
                );

            } finally {
                Util.close(cursor);
            }

//        long                            t2 = System.currentTimeMillis ();
//
//        s = (t2 - t0) * 0.001;
//        System.out.printf (
//            "Overall: %,d messages in %,.3fs; speed: %,.0f msg/s\n",
//            count,
//            s,
//            count / s
//        );
        }
    }

    private class MessageGenerator implements Runnable {
        private static final long MAX_FEED_RATE_FOR_SPIN_LOCK = 10000;
        private final TickLoader loader;
        private String[]    table = new String[32];
        private int[]       values = new int[32];

        private final int total;
        private int throughputMessagesPerSecond;
        private final String[] symbols;

        public volatile boolean active = true;

        final RawMessage trade = new RawMessage();

        private MessageGenerator(TickLoader loader, int total, int throughputMessagesPerSecond, int messageSize, String[] symbols) {
            this.loader = loader;
            this.total = total;
            this.throughputMessagesPerSecond = throughputMessagesPerSecond;
            this.symbols = symbols;

            trade.type = descriptor;
            trade.data = new byte[messageSize];
            trade.setSymbol ("DLTX");
        }

        @Override
        public void run() {
            final long intervalBetweenMessagesInNanos;

            intervalBetweenMessagesInNanos = TimeUnit.SECONDS.toNanos(1) / throughputMessagesPerSecond;

            long count = 0;

            long                            t0 = System.currentTimeMillis ();

            while (count <= total) {

                long nextNanoTime = (intervalBetweenMessagesInNanos != 0) ? System.nanoTime() + intervalBetweenMessagesInNanos : 0;
                while (count <= total) {
                    if (intervalBetweenMessagesInNanos != 0) {
                        if (System.nanoTime() < nextNanoTime)
                            continue; // spin-wait
                    }

                    trade.setSymbol (symbols[(int)(count % symbols.length)]);
                    //trade.setNanoTime(System.nanoTime());
                    //trade.timestamp = System.nanoTime();
                    loader.send (trade);

                    nextNanoTime += intervalBetweenMessagesInNanos;
                    count++;
                }
            }


            long                            t1 = System.currentTimeMillis ();
            double                          s = (t1 - t0) * 0.001;
            System.out.printf (
                    "Write %,d messages in %,.3fs; speed: %,.0f msg/s\n",
                    count,
                    s,
                    count / s
            );

        }
    }

    public static void main(String[] args) {
        new TestPerformance(args).start();
    }

}