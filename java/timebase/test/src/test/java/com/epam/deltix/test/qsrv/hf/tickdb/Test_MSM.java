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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.data.stream.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer.PrioritizedMessageSourceMultiplexer;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.util.concurrent.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class Test_MSM {

    @Test
    public void testSyntheticPerformance() throws Exception {
        syntheticPerformance(10);
    }

    private void syntheticPerformance(int iterations) throws Exception {
        for (int k = 1; k <= iterations; k++) {
            System.out.println("Iteration " + k);

            MessageSourceMultiplexer<InstrumentMessage> mx =
                    new IAMessageSourceMultiplexer<>(true, false);

            mx.add(new TestSource());
            mx.add(new IASource());

            final long total = 100000000;
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < total; i++)
                mx.next();
            long t1 = System.currentTimeMillis();

            double s = (t1 - t0) * 0.001;
            System.out.printf("MSM Normal mode: %,d messages in %,.3fs; speed: %,.0f msg/s\n", total, s, total / s);


            mx = new MessageSourceMultiplexer<>(true, true);
            mx.add(new TestSource());
            mx.add(new IASource());

            t0 = System.currentTimeMillis();
            for (int i = 0; i < total; i++)
                mx.next();
            t1 = System.currentTimeMillis();

            s = (t1 - t0) * 0.001;
            System.out.printf("MSM Realtime mode: %,d messages in %,.3fs; speed: %,.0f msg/s\n", total, s, total / s);


            TestSource source = new TestSource();
            t0 = System.currentTimeMillis();
            for (int i = 0; i < total; i++)
                source.next();
            t1 = System.currentTimeMillis();

            s = (t1 - t0) * 0.001;
            System.out.printf("Single Source: %,d messages in %,.3fs; speed: %,.0f msg/s\n", total, s, total / s);

            //

            PrioritizedMessageSourceMultiplexer<InstrumentMessage> pmx = new PrioritizedMessageSourceMultiplexer<>(true, true);
            pmx.add(new TestSource());
            pmx.add(new IASource());

            t0 = System.currentTimeMillis();
            for (int i = 0; i < total; i++)
                pmx.next();
            t1 = System.currentTimeMillis();

            s = (t1 - t0) * 0.001;
            System.out.printf("Prioritized MSM Realtime mode: %,d messages in %,.3fs; speed: %,.0f msg/s\n", total, s, total / s);

            System.out.println();
        }
    }

    public static void testSyntheticPerformance(int numSources, int iterations) throws Exception {
        final int total = 1_000_000_000;

        for (int k = 1; k <= iterations; k++) {
            System.out.println("Iteration " + k);

            testMSM_noRealTime(numSources, total);

            testMSM_realtime(numSources, total);

            testFixedMSM(numSources, total);

            testSingleSource(total);

            testPrioritizedMSM_noRealTime(numSources, total);

            testPrioritizedMSM_realtime(numSources, total);
        }
    }

    private static void testMSM_noRealTime(int numSources, int total) {
        MessageSourceMultiplexer<InstrumentMessage> mx =
                new MessageSourceMultiplexer<>(true, false);

        for (int i = 0; i < numSources; i++)
            mx.add(new TestSource());


        long t0 = System.currentTimeMillis();
        for (int i = 0; i < total; i++)
            mx.next();
        long t1 = System.currentTimeMillis();

        double s = (t1 - t0) * 0.001;
        double n = TimeUnit.MILLISECONDS.toNanos(t1 - t0);
        System.out.printf("MSM Normal mode[%s source(s)]: %,d messages in %,.3fs; speed: %,.0f msg/s; avg: %,.0f ns/msg\n", numSources, total, s, total / s, n / total);
    }

    private static void testMSM_realtime(int numSources, int total) {
        MessageSourceMultiplexer<InstrumentMessage> mx = new MessageSourceMultiplexer<>(true, true);

        for (int i = 0; i < numSources; i++)
            mx.add(new TestSource());

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < total; i++)
            mx.next();
        long t1 = System.currentTimeMillis();

        double s = (t1 - t0) * 0.001;
        double n = TimeUnit.MILLISECONDS.toNanos(t1 - t0);
        System.out.printf("MSM Realtime mode[%s source(s)]: %,d messages in %,.3fs; speed: %,.0f msg/s; avg: %,.0f ns/msg\n", numSources, total, s, total / s, n / total);
    }

    private static void testFixedMSM(int numSources, int total) {
        ArrayList<MessageSource<InstrumentMessage>> srcs = new ArrayList<>();
        for (int i = 0; i < numSources; i++) {
            srcs.add(new TestSource());
        }
        long t0 = System.currentTimeMillis();
        Object lock = new Object();
        MessageSourceMultiplexerFixed<InstrumentMessage> fmx = new MessageSourceMultiplexerFixed<>(null, srcs, true, Long.MIN_VALUE, lock);
        for (int i = 0; i < total; i++) {
            fmx.next();
        }
        long t1 = System.currentTimeMillis();

        double s = (t1 - t0) * 0.001;
        double n = TimeUnit.MILLISECONDS.toNanos(t1 - t0);
        System.out.printf("FixedMSM [%s source(s)]: %,d messages in %,.3fs; speed: %,.0f msg/s; avg: %,.0f ns/msg\n", numSources, total, s, total / s, n / total);
    }

    private static void testSingleSource(int total) {
        TestSource source = new TestSource();
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < total; i++)
            source.next();
        long t1 = System.currentTimeMillis();

        double s = (t1 - t0) * 0.001;
        double n = TimeUnit.MILLISECONDS.toNanos(t1 - t0);
        System.out.printf("Single Source: %,d messages in %,.3fs; speed: %,.0f msg/s; avg: %,.0f ns/msg\n", total, s, total / s, n / total);
    }

    private static void testPrioritizedMSM_noRealTime(int numSources, int total) {
        PrioritizedMessageSourceMultiplexer<InstrumentMessage> mx =
                new PrioritizedMessageSourceMultiplexer<>(true, false);

        for (int i = 0; i < numSources; i++)
            mx.addWithPriority(new TestSource(), i);


        long t0 = System.currentTimeMillis();
        for (int i = 0; i < total; i++)
            mx.next();
        long t1 = System.currentTimeMillis();

        double s = (t1 - t0) * 0.001;
        double n = TimeUnit.MILLISECONDS.toNanos(t1 - t0);
        System.out.printf("PrioritizedMSM Normal mode[%s source(s)]: %,d messages in %,.3fs; speed: %,.0f msg/s; avg: %,.0f ns/msg\n", numSources, total, s, total / s, n / total);
    }

    private static void testPrioritizedMSM_realtime(int numSources, int total) {
        PrioritizedMessageSourceMultiplexer<InstrumentMessage> mx = new PrioritizedMessageSourceMultiplexer<>(true, true);

        for (int i = 0; i < numSources; i++)
            mx.addWithPriority(new TestSource(), i);

        long t0 = System.currentTimeMillis();
        for (int i = 0; i < total; i++)
            mx.next();
        long t1 = System.currentTimeMillis();

        double s = (t1 - t0) * 0.001;
        double n = TimeUnit.MILLISECONDS.toNanos(t1 - t0);
        System.out.printf("PrioritizedMSM Realtime mode[%s source(s)]: %,d messages in %,.3fs; speed: %,.0f msg/s; avg: %,.0f ns/msg\n", numSources, total, s, total / s, n / total);
    }

    public static void main(String[] args) throws Throwable {
        testSyntheticPerformance(8, 10);
    }

    public static class TestSource implements RealTimeMessageSource<InstrumentMessage>, IntermittentlyAvailableCursor {
        private final BarMessage message = new BarMessage();
        private boolean isRealTime;

        public TestSource() {
            message.setSymbol("DLTX");
        }

        @Override
        public boolean isRealTime() {
            return isRealTime;
        }

        @Override
        public boolean realTimeAvailable() {
            return true;
        }

        @Override
        public InstrumentMessage getMessage() {
            return message;
        }

        @Override
        public boolean next() {
            message.setTimeStampMs(message.getTimeStampMs() + 1);
            isRealTime = message.getTimeStampMs() > 10000;
            return true;
        }

        @Override
        public NextResult nextIfAvailable() {
            return NextResult.OK;
        }

        @Override
        public boolean isAtEnd() {
            return false;
        }

        @Override
        public void close() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    public static class IASource implements RealTimeMessageSource<InstrumentMessage>, IntermittentlyAvailableResource, IntermittentlyAvailableCursor {
        private final BarMessage    message = new BarMessage();
        private boolean             isRealTime;

        private Runnable            listener;

        final QuickExecutor.QuickTask     notifier =
                new QuickExecutor.QuickTask () {
                    @Override
                    public void run () {
                        listener.run();
                    }
                };

        public IASource() {
            message.setSymbol("DLTX");
        }

        @Override
        public boolean isRealTime() {
            return isRealTime;
        }

        @Override
        public boolean realTimeAvailable() {
            return true;
        }

        @Override
        public InstrumentMessage getMessage() {
            return message;
        }

        @Override
        public boolean next() {
            message.setTimeStampMs(message.getTimeStampMs() + 1);
            isRealTime = message.getTimeStampMs() > 10000;

            if (isRealTime && message.getTimeStampMs() % 100 == 0) {
                notifier.submit();
                throw UnavailableResourceException.INSTANCE;
            }

            return true;
        }

        @Override
        public NextResult nextIfAvailable() {
            message.setTimeStampMs(message.getTimeStampMs() + 1);
            isRealTime = message.getTimeStampMs() > 10000;

            if (isRealTime && message.getTimeStampMs() % 100 == 0) {
                notifier.submit();
                return NextResult.UNAVAILABLE;
            }

            return NextResult.OK;
        }

        @Override
        public boolean isAtEnd() {
            return false;
        }

        @Override
        public void setAvailabilityListener(Runnable maybeAvailable) {
            listener = maybeAvailable;
        }

        @Override
        public void close() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
