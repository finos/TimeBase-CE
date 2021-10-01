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
import com.epam.deltix.data.stream.pq.BucketQueue;
import com.epam.deltix.data.stream.pq.RegularBucketQueue;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.queue.sortedarray.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.queue.binaryheap.LongPriorityQueue;
import com.epam.deltix.data.stream.pq.PriorityQueueExt;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.queue.binaryheap.PriorityQueueGeneralizedWithData;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.queue.binaryheap.PriorityQueueGeneralizedWithDataNoHeadRemoval;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.utilityclasses.MessageSourceComparator;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.utilityclasses.MessageTimeComparator;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.utilityclasses.TimeStampedMessageMessageSource;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import org.junit.Assert;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
@Warmup(time = 10, timeUnit = TimeUnit.SECONDS, iterations = 2)
@Measurement(time = 30, timeUnit = TimeUnit.SECONDS, iterations = 1)
@BenchmarkMode(Mode.AverageTime)
//@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(1)
@Threads(1)
public class MiltiplexingQueueBenchmark {
    //@Benchmark
    public long benchBaseline(BaselineState state) throws Exception {
        state.source.next();
        long nanoTime = state.source.getMessage().getNanoTime();
        return nanoTime;
    }

    //@Benchmark
    public MessageSource<TimeStampedMessage> benchPriorityQueue(PriorityQueueState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source);
        return source;
    }

    @Benchmark
    public MessageSource<TimeStampedMessage> benchBasicBucketTimeQueue(BasicBucketTimeQueueState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source, source.getMessage().getNanoTime());
        return source;
    }

    @Benchmark
    public MessageSource<TimeStampedMessage> benchCustomTimeBucketTimeQueueWithNanos(CustomTimeBucketTimeQueueWithNanosState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source, source.getMessage().getNanoTime());
        return source;
    }

    @Benchmark
    public MessageSource<TimeStampedMessage> benchCustomTimeBucketTimeQueueWithMs(CustomTimeBucketTimeQueueWithMsState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source, source.getMessage().getNanoTime());
        return source;
    }

    @Benchmark
    public MessageSource<TimeStampedMessage> benchCustomTimeBucketTimeQueueWithMsExploded(CustomTimeBucketTimeQueueWithMsExplodedState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source, source.getMessage().getNanoTime());
        return source;
    }

    //@Benchmark
    public MessageSource<TimeStampedMessage> benchPriorityQueueGeneralizedWithData(PriorityQueueGeneralizedWithDataState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source, source.getMessage().getNanoTime());
        return source;
    }

    //@Benchmark
    public MessageSource<TimeStampedMessage> benchPriorityQueueGeneralizedWithDataNoHeadRemoval(PriorityQueueGeneralizedWithDataNoHeadRemovalState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.takeHead();
        source.next();
        state.queue.putBack(source, source.getMessage().getNanoTime());
        return source;
    }

    //@Benchmark
    public MessageSource<TimeStampedMessage> benchPriorityQueueExt(PriorityQueueExtState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source, source.getMessage().getNanoTime());
        return source;
    }

    //@Benchmark
    public MessageSource<TimeStampedMessage> benchSimpleFixedPriorityQueue(SimpleFixedPriorityQueueState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source);
        return source;
    }

    //@Benchmark
    public MessageSource<TimeStampedMessage> benchNoMoveFixedPriorityQueue(NoMoveFixedPriorityQueueState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source, source.getMessage().getNanoTime());
        return source;
    }

    //@Benchmark
    public MessageSource<TimeStampedMessage> benchObjectFixedPriorityQueue(ObjectFixedPriorityQueueState state) throws Exception {
        Object sourceObj = state.queue.poll();
        TimeStampedMessageMessageSource source = (TimeStampedMessageMessageSource) sourceObj;
        source.next();
        state.queue.offer(source, source.getMessage().getNanoTime());
        return source;
    }

    //@Benchmark //1
    public long benchNoSourceFixedPriorityQueue(NoSourceFixedPriorityQueueState state) throws Exception {
        long prevTime = state.queue.poll();
        long newTime = getNextRandomTime(prevTime, state.random, state.step);
        state.queue.offer(newTime);
        return newTime;
    }

    //@Benchmark
    public long benchLongPriorityQueue(LongPriorityQueueState state) throws Exception {
        long prevTime = state.queue.poll();
        long newTime = getNextRandomTime(prevTime, state.random, state.step);
        state.queue.offer(newTime);
        return newTime;
    }

    //@Benchmark
    public long benchLongPriorityQueue2(LongPriorityQueueState state) throws Exception {
        long prevTime = state.queue.poll();
        long newTime = getNextRandomTime(prevTime, state.random, state.step);
        state.source.next();
        state.queue.offer(newTime);
        return newTime;
    }

    //@Benchmark
    public long benchLongPriorityQueue3(LongPriorityQueueState state) throws Exception {
        long prevTime = state.queue.poll();
        long newTime = getNextRandomTime(prevTime, state.random, state.step);
        state.source.next();
        TimeStampedMessage message = state.source.getMessage();
        state.queue.offer(newTime);
        return message.getNanoTime();
    }

    //@Benchmark
    public long benchLongPriorityQueue4(LongPriorityQueueObjectState state) throws Exception {
        long prevTime = state.queue.poll();
        long newTime = getNextRandomTime(prevTime, state.random, state.step);
        MessageSource<TimeStampedMessage> source = (MessageSource<TimeStampedMessage>) state.source;
        source.next();
        TimeStampedMessage message = source.getMessage();
        state.queue.offer(newTime);
        return message.getNanoTime();
    }

    //@Benchmark
    public TimeStampedMessageMessageSource benchNoMoveObjectFixedPriorityQueue1(NoMoveObjectFixedPriorityQueueState state) throws Exception {
        Object sourceObj = state.queue.poll();
        TimeStampedMessageMessageSource source = (TimeStampedMessageMessageSource) sourceObj;
        source.next();
        long nanoTime = source.getMessage().getNanoTime();
        state.queue.offer(nanoTime);
        return source;
    }

    //@Benchmark
    public MessageSource<TimeStampedMessage> benchJavaPriorityQueue(JavaPriorityQueueState state) throws Exception {
        MessageSource<TimeStampedMessage> source = state.queue.poll();
        source.next();
        state.queue.offer(source);
        return source;
    }

    @State(Scope.Thread)
    public static class BenchmarkSettings {
        //@Param({"1", "2", "10", "100", "250", "1000"})
        //@Param({"1", "1000"})
        //@Param({"1", "2", "6", "10", "100", "1000"})
        @Param({"1000"})
        int messageSourceCount;

        @Param({"10"})
        int step;

        List<MessageSource<TimeStampedMessage>> sources = new ArrayList<>();
        Random random = new Random(0);

        List<MessageSource<TimeStampedMessage>> sourcesMs = new ArrayList<>();
        Random randomMs = new Random(0);

        @Setup
        public void prepare() throws Exception {

            long baseTimestamp = 1000000000000L; // ~2001.09.09
            for (int i = 0; i < messageSourceCount; i++) {
                long baseTimestampForSource = baseTimestamp + random.nextInt((messageSourceCount * 2 + 1) / 2);
                TimeStampedMessageMessageSource source = new TimeStampedMessageMessageSource(baseTimestampForSource, random, step, 1);
                source.next();
                sources.add(source);
            }
            long nanosInMs = TimeUnit.MILLISECONDS.toNanos(0);
            for (int i = 0; i < messageSourceCount; i++) {
                long baseTimestampForSource = baseTimestamp + randomMs.nextInt((messageSourceCount * 2 + 1) / 2) * nanosInMs;
                TimeStampedMessageMessageSource source = new TimeStampedMessageMessageSource(baseTimestampForSource, randomMs, step, nanosInMs);
                source.next();
                sourcesMs.add(source);
            }
        }
    }

    @State(Scope.Thread)
    public static class PriorityQueueState {
        final PriorityQueue<TimeStampedMessage> queue = new PriorityQueue<TimeStampedMessage>(256, true, new MessageTimeComparator<>());

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            for (MessageSource<TimeStampedMessage> source : settings.sources) {
                queue.offer(source);
            }
        }

    }

    @State(Scope.Thread)
    public static class PriorityQueueGeneralizedWithDataState {
        final PriorityQueueGeneralizedWithData<MessageSource<TimeStampedMessage>> queue = new PriorityQueueGeneralizedWithData<>(256, true);

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            for (MessageSource<TimeStampedMessage> source : settings.sources) {
                queue.offer(source, source.getMessage().getNanoTime());
            }
        }

    }

    @State(Scope.Thread)
    public static class PriorityQueueGeneralizedWithDataNoHeadRemovalState {
        final PriorityQueueGeneralizedWithDataNoHeadRemoval<MessageSource<TimeStampedMessage>> queue = new PriorityQueueGeneralizedWithDataNoHeadRemoval<>(256, true);

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            for (MessageSource<TimeStampedMessage> source : settings.sources) {
                queue.offer(source, source.getMessage().getNanoTime());
            }
        }

    }

    @State(Scope.Thread)
    public static class PriorityQueueExtState {
        final PriorityQueueExt<MessageSource<TimeStampedMessage>> queue = new PriorityQueueExt<>(256, true);

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            for (MessageSource<TimeStampedMessage> source : settings.sources) {
                queue.offer(source, source.getMessage().getNanoTime());
            }
        }

    }

    @State(Scope.Thread)
    public static class JavaPriorityQueueState {
        final java.util.PriorityQueue<MessageSource<TimeStampedMessage>> queue = new java.util.PriorityQueue<>(new MessageSourceComparator());

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            for (MessageSource<TimeStampedMessage> source : settings.sources) {
                queue.offer(source);
            }
        }
    }

    @State(Scope.Thread)
    public static class SimpleFixedPriorityQueueState {
        SimpleFixedPriorityQueue<TimeStampedMessage> queue;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            queue = new SimpleFixedPriorityQueue<>(settings.sources);
        }
    }

    @State(Scope.Thread)
    public static class ObjectFixedPriorityQueueState {
        ObjectFixedPriorityQueue queue;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            queue = new ObjectFixedPriorityQueue(settings.sources);
        }
    }

    @State(Scope.Thread)
    public static class NoMoveFixedPriorityQueueState {
        NoMoveFixedPriorityQueue<TimeStampedMessage> queue;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            queue = new NoMoveFixedPriorityQueue<>(settings.sources);
        }
    }

    @State(Scope.Thread)
    public static class BasicBucketTimeQueueState {
        BucketQueue<MessageSource<TimeStampedMessage>> queue;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            queue = new BucketQueue<>(1000, true);
            for (MessageSource<TimeStampedMessage> source : settings.sources) {
                queue.offer(source, source.getMessage().getNanoTime());
            }
        }
    }

    @State(Scope.Thread)
    public static class CustomTimeBucketTimeQueueWithNanosState {
        RegularBucketQueue<MessageSource<TimeStampedMessage>> queue;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            queue = new RegularBucketQueue<>(1000, TimeUnit.MILLISECONDS.toNanos(1), true);
            for (MessageSource<TimeStampedMessage> source : settings.sources) {
                queue.offer(source, source.getMessage().getNanoTime());
            }
        }
    }

    @State(Scope.Thread)
    public static class CustomTimeBucketTimeQueueWithMsState {
        RegularBucketQueue<MessageSource<TimeStampedMessage>> queue;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            queue = new RegularBucketQueue<>(1000, TimeUnit.MILLISECONDS.toNanos(1), true);
            for (MessageSource<TimeStampedMessage> source : settings.sourcesMs) {
                queue.offer(source, source.getMessage().getNanoTime());
            }
            Assert.assertFalse(queue.isMixedKeysDetected());
        }
    }

    @State(Scope.Thread)
    public static class CustomTimeBucketTimeQueueWithMsExplodedState {
        RegularBucketQueue<MessageSource<TimeStampedMessage>> queue;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            queue = new RegularBucketQueue<>(1000, TimeUnit.MILLISECONDS.toNanos(1), true);
            for (MessageSource<TimeStampedMessage> source : settings.sourcesMs) {
                queue.offer(source, source.getMessage().getNanoTime());
            }

            // Get first source to find out lowest timestamp
            MessageSource<TimeStampedMessage> source = queue.poll();
            long firstTs = source.getMessage().getNanoTime();
            queue.offer(source, firstTs);

            // Add and remove 2 sources that will get into one bucket and trigger switch to PriorityQueueExt
            queue.offer(source, firstTs - 1);
            queue.offer(source, firstTs - 2);
            queue.poll();
            queue.poll();
            Assert.assertTrue(queue.isMixedKeysDetected());
        }
    }

    @State(Scope.Thread)
    public static class NoSourceFixedPriorityQueueState {
        NoSourceFixedPriorityQueue<TimeStampedMessage> queue;
        int step;
        Random random = new Random(0);

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            this.step = settings.step;
            this.random = settings.random;
            queue = new NoSourceFixedPriorityQueue<>(settings.sources);
        }
    }

    @State(Scope.Thread)
    public static class LongPriorityQueueState {
        LongPriorityQueue queue;
        int step;
        Random random = new Random(0);
        MessageSource<TimeStampedMessage> source;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            this.step = settings.step;
            this.random = settings.random;
            queue = new LongPriorityQueue(settings.sources.size(), true);
            for (MessageSource<TimeStampedMessage> source : settings.sources) {
                queue.offer(source.getMessage().getNanoTime());
            }

            source = settings.sources.iterator().next();
        }
    }
    @State(Scope.Thread)
    public static class LongPriorityQueueObjectState {
        LongPriorityQueue queue;
        int step;
        Random random = new Random(0);
        Object source;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            this.step = settings.step;
            this.random = settings.random;
            for (MessageSource<TimeStampedMessage> source : settings.sources) {
                queue.offer(source.getMessage().getNanoTime());
            }
            queue = new LongPriorityQueue(settings.sources.size(), true);
            source = settings.sources.iterator().next();
        }
    }

    @State(Scope.Thread)
    public static class NoMoveObjectFixedPriorityQueueState {
        NoMoveObjectFixedPriorityQueue queue;
        int step;
        Random random = new Random(0);

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            this.step = settings.step;
            this.random = settings.random;
            queue = new NoMoveObjectFixedPriorityQueue(settings.sources);
        }
    }

    @State(Scope.Thread)
    public static class BaselineState {
        MessageSource<TimeStampedMessage> source;

        @Setup
        public void prepare(BenchmarkSettings settings) throws Exception {
            source = settings.sources.iterator().next();
        }
    }

    public static long getNextRandomTime(long prevTime, Random random, int step, long multiplier) {
        return TimeStampedMessageMessageSource.getNextRandomTime(prevTime, random, step, multiplier);
    }

    public static long getNextRandomTime(long prevTime, Random random, int step) {
        return TimeStampedMessageMessageSource.getNextRandomTime(prevTime, random, step);
    }


    public static void main(String[] args) throws RunnerException {
        String simpleName = MiltiplexingQueueBenchmark.class.getSimpleName();
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