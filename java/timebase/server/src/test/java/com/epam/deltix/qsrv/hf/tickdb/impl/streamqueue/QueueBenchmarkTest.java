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
package com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue;

import com.epam.deltix.data.stream.PriorityQueue;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.data.stream.pq.PriorityQueueExt;
import com.epam.deltix.data.stream.pq.BucketQueue;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.utilityclasses.MessageTimeComparator;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import org.junit.Test;

import java.time.LocalDate;

public class QueueBenchmarkTest {

    private final class TSGenerator {
        public long start;
        public int step;

        public TSGenerator(long start, int step) {
            this.start = start;
            this.step = step;
        }

        public long next() {
            start += step;
            return start;
        }
    }

    public final class TestMessageSource implements MessageSource<TimeStampedMessage> {
        private final InstrumentMessage message = new InstrumentMessage();
        private TSGenerator g;

        public TestMessageSource(TSGenerator g) {
            this.g = g;
        }

        @Override
        public InstrumentMessage getMessage() {
            return message;
        }

        @Override
        public boolean next() {
            long newTime = g.next();
            message.setTimeStampMs(newTime);
            return true;
        }

        @Override
        public boolean isAtEnd() {
            return false;
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return "TSM{" +
                    "ts=" + message.getNanoTime() +
                    '}';
        }
    }

    void    printElapsedTime(String info, long start, long end, int count) {
        double                          s = (System.currentTimeMillis() - start) * 0.001;
        System.out.printf (
                "%s\t\n  %,d messages in %,.3fs; speed: %,.0f msg/s \n",
                info,
                count,
                s,
                count / s
        );
    }

    public void readQueue(PriorityQueue<TimeStampedMessage> pq, int count) {

        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            MessageSource<TimeStampedMessage> s1 = pq.poll();

            s1.next();
            long time = s1.getMessage().getTimeStampMs();
            pq.offer(s1);
        }

        printElapsedTime(pq.getClass().getName(), start, System.currentTimeMillis(), count);

    }

    public void readQueue(PriorityQueueExt<MessageSource<TimeStampedMessage>> pq, int count) {

        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            MessageSource<TimeStampedMessage> s1 = pq.poll();

            s1.next();
            long time = s1.getMessage().getTimeStampMs();
            pq.offer(s1, time);
        }

        printElapsedTime(pq.getClass().getName(), start, System.currentTimeMillis(), count);
    }

    public void readQueue(BucketQueue<MessageSource<TimeStampedMessage>> pq, int count) {

        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            MessageSource<TimeStampedMessage> s1 = pq.poll();

            s1.next();
            long time = s1.getMessage().getTimeStampMs();
            pq.offer(s1, time);
        }

        printElapsedTime(pq.getClass().getName(), start, System.currentTimeMillis(), count);
    }

    private TestMessageSource createSource(long timestamp, int step) {
        return new TestMessageSource(new TSGenerator(timestamp, step));
    }

    @Test
    public void     testPerformance() {
        testPerformance(1, 100_000_000);
        testPerformance(2, 100_000_000);
        testPerformance(5, 100_000_000);
        testPerformance(10, 100_000_000);
        testPerformance(100, 100_000_000);
        testPerformance(1000, 100_000_000);
        testPerformance(10000, 100_000_000);
    }

    public void testPerformance(int queueSize, int iterations) {
        System.out.printf ("\nRunning performance test[Queue size = %d, iterations = %,d]\n", queueSize, iterations);

        MessageTimeComparator c = new MessageTimeComparator();
        PriorityQueue<TimeStampedMessage> pq1 = new PriorityQueue<TimeStampedMessage>(256, true, c);
        PriorityQueueExt<MessageSource<TimeStampedMessage>> pq2 = new PriorityQueueExt<>(256, true);
        BucketQueue<MessageSource<TimeStampedMessage>> pq3 = new BucketQueue<>(15000, true);

        long baseTimestamp = LocalDate.of(2017, 1, 1).toEpochDay() * 24 * 3600;

        // Init streams
        int step = queueSize;
        for (int i = 0; i < queueSize; i++) {
            long time = baseTimestamp + i;

            TestMessageSource src = createSource(time, step);
            src.next();
            pq1.offer(src);

            src = createSource(time, step);
            src.next();
            pq2.offer(src, src.getMessage().getTimeStampMs());

            src = createSource(time, step);
            src.next();
            pq3.offer(src, src.getMessage().getTimeStampMs());
        }

        readQueue(pq1, iterations);
        readQueue(pq2, iterations);
        readQueue(pq3, iterations);
    }
}