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
package com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.PriorityQueue;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.data.stream.pq.PriorityQueueExt;
import com.epam.deltix.data.stream.pq.BucketQueue;
import com.epam.deltix.data.stream.pq.RegularBucketQueue;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.utilityclasses.MessageTimeComparator;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.LocalDate;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Warmup(time = 5, timeUnit = TimeUnit.SECONDS, iterations = 1)
@Measurement(time = 10, timeUnit = TimeUnit.SECONDS, iterations = 1)
@BenchmarkMode(Mode.Throughput)
//@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Threads(3)
public class QueueBenchmarkJmh {


    @State(Scope.Thread)
    public static class BenchmarkState {
        @Param({"1", "2", "5", "10", "100", "1000", "10000"})
        //@Param({"1000"})
        int queueSize;

        MessageTimeComparator c = new MessageTimeComparator();
        PriorityQueue<TimeStampedMessage> pq1 = new PriorityQueue<TimeStampedMessage>(256, true, c);
        PriorityQueueExt<MessageSource<TimeStampedMessage>> pq2 = new PriorityQueueExt<>(256, true);
        BucketQueue<MessageSource<TimeStampedMessage>> pq3 = new BucketQueue<>(1500, true);
        RegularBucketQueue<MessageSource<TimeStampedMessage>> pq4 = new RegularBucketQueue<>(1500, 1, true);
        RegularBucketQueue<MessageSource<TimeStampedMessage>> pq5 = new RegularBucketQueue<>(1500, TimeUnit.MILLISECONDS.toNanos(1), true);

        long baseTimestamp = LocalDate.of(2017, 1, 1).toEpochDay() * 24 * 3600;

        // Init streams

        @Setup
        public void prepare() throws Exception {
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

                src = createSource(time, step);
                src.next();
                pq4.offer(src, src.getMessage().getTimeStampMs());

                src = createSource(time, step);
                src.next();
                pq5.offer(src, src.getMessage().getNanoTime());
            }
        }
    }

    @Benchmark
    public long benchPriorityQueue(BenchmarkState state) throws Exception {
        PriorityQueue<TimeStampedMessage> pq = state.pq1;

        MessageSource<TimeStampedMessage> s1 = pq.poll();
        s1.next();
        long time = s1.getMessage().getTimeStampMs();
        pq.offer(s1);
        return time;
    }

    @Benchmark
    public long benchPriorityQueueExt(BenchmarkState state) throws Exception {
        PriorityQueueExt<MessageSource<TimeStampedMessage>> pq = state.pq2;

        MessageSource<TimeStampedMessage> s1 = pq.poll();
        s1.next();
        long time = s1.getMessage().getTimeStampMs();
        pq.offer(s1, time);
        return time;
    }

    @Benchmark
    public long benchBasicBucketTimeQueue(BenchmarkState state) throws Exception {
        BucketQueue<MessageSource<TimeStampedMessage>> pq = state.pq3;

        MessageSource<TimeStampedMessage> s1 = pq.poll();
        s1.next();
        long time = s1.getMessage().getTimeStampMs();
        pq.offer(s1, time);
        return time;
    }

    @Benchmark
    public long benchCustomTimeBucketTimeQueueSize1(BenchmarkState state) throws Exception {
        RegularBucketQueue<MessageSource<TimeStampedMessage>> pq = state.pq4;

        MessageSource<TimeStampedMessage> s1 = pq.poll();
        s1.next();
        long time = s1.getMessage().getTimeStampMs();
        pq.offer(s1, time);
        return time;
    }

    @Benchmark
    public long benchCustomTimeBucketTimeQueue(BenchmarkState state) throws Exception {
        RegularBucketQueue<MessageSource<TimeStampedMessage>> pq = state.pq5;

        MessageSource<TimeStampedMessage> s1 = pq.poll();
        s1.next();
        long timeNanos = s1.getMessage().getNanoTime();
        pq.offer(s1, timeNanos);
        return timeNanos;
    }


    private static final class TSGenerator {
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

    public static final class TestMessageSource implements MessageSource<TimeStampedMessage> {
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

    private static TestMessageSource createSource(long timestamp, int step) {
        return new TestMessageSource(new TSGenerator(timestamp, step));
    }

    public static void main(String[] args) throws RunnerException {
        String simpleName = QueueBenchmarkJmh.class.getSimpleName();
        Options opt = new OptionsBuilder()
                .include(simpleName)
                //.forks(1)
                //.syncIterations(false)

                //.addProfiler(WinPerfAsmProfiler.class)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .result(simpleName + "_report.json")
                .resultFormat(ResultFormatType.JSON)
                .build();

        Collection<RunResult> runResults = new Runner(opt).run();
        System.out.println(runResults);
    }
}