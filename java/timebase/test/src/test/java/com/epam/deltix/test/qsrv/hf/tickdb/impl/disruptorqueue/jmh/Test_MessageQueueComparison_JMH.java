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
package com.epam.deltix.test.qsrv.hf.tickdb.impl.disruptorqueue.jmh;

import com.epam.deltix.test.qsrv.hf.tickdb.impl.disruptorqueue.Test_MessageQueueComparison;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.lang.Disposable;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Control;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.epam.deltix.test.qsrv.hf.tickdb.Test_MemoryExchangeThroughput.createRawMessage;

/**
 * <p>JMH benchmark.
 * See <a href="http://tutorials.jenkov.com/java-performance/jmh.html">http://tutorials.jenkov.com/java-performance/jmh.html</a></p>
 *
 * <p>Use {@link #main} method to run and generate report.</p>
 *
 * @author Alexei Osipov
 */
@Warmup(time = 10, timeUnit = TimeUnit.SECONDS, iterations = 1)
@Measurement(time = 10, timeUnit = TimeUnit.SECONDS, iterations = 3)
@BenchmarkMode(Mode.Throughput)
@Fork(1) // Set this value to 0 if you want to debug this test
// TODO: Move to timebase-server module
@SuppressWarnings("DefaultAnnotationParam")
public class Test_MessageQueueComparison_JMH {

    @State(Scope.Group)
    public static class ConfigState {
        //@Param({"false", "true"})
        @Param({"false"})
        boolean remote;

        @Param({"false", "true"})
        boolean lossless;

        @Param({"false", "true"})
        boolean disruptor;

        //@Param({"8", "64", "128"})
        @Param({"128"})
        int queueSizeKb;
    }


    // C1
    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C1")
    public void measureProducerP1C1(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        coordinationState.init(1, 1);
        producerIteration(producerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C1")
    public void measureConsumerP1C1(ConsumerState consumerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        consumerIteration(consumerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C1")
    public void arbiterP1C1(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        arbiterIteration(producerState, coordinationState, control);
    }


    // C2
    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C2")
    public void measureProducerP1C2(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        coordinationState.init(1, 2); // Pass number of producers and consumers to coordination state
        producerIteration(producerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(2)
    @Group("testThroughputP1C2")
    public void measureConsumerP1C2(ConsumerState consumerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        consumerIteration(consumerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C2")
    public void arbiterP1C2(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        arbiterIteration(producerState, coordinationState, control);
    }


    // C4
    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C4")
    public void measureProducerP1C4(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        coordinationState.init(1, 4);
        producerIteration(producerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(4)
    @Group("testThroughputP1C4")
    public void measureConsumerP1C4(ConsumerState consumerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        consumerIteration(consumerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C4")
    public void arbiterP1C4(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        arbiterIteration(producerState, coordinationState, control);
    }


    // C8
    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C8")
    public void measureProducerP1C8(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        coordinationState.init(1, 8);
        producerIteration(producerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(8)
    @Group("testThroughputP1C8")
    public void measureConsumerP1C8(ConsumerState consumerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        consumerIteration(consumerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C8")
    public void arbiterP1C8(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        arbiterIteration(producerState, coordinationState, control);
    }

    // P2C1
    @Benchmark
    @GroupThreads(2)
    @Group("testThroughputP2C1")
    public void measureProducerP2C1(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        coordinationState.init(2, 1);
        producerIteration(producerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP2C1")
    public void measureConsumerP2C1(ConsumerState consumerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        consumerIteration(consumerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP2C1")
    public void arbiterP2C1(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        arbiterIteration(producerState, coordinationState, control);
    }

    // P2C2
    @Benchmark
    @GroupThreads(2)
    @Group("testThroughputP2C2")
    public void measureProducerP2C2(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        coordinationState.init(2, 2);
        producerIteration(producerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(2)
    @Group("testThroughputP2C2")
    public void measureConsumerP2C2(ConsumerState consumerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        consumerIteration(consumerState, coordinationState, control);
    }

    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP2C2")
    public void arbiterP2C2(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        arbiterIteration(producerState, coordinationState, control);
    }

    private void producerIteration(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        if (control.stopMeasurement) {
            Thread.sleep(1);
            return;
        }
        //coordinationState.producerInvocationCount.incrementAndGet();
        coordinationState.producersBlocked.incrementAndGet();
        try {
            producerState.loader.send(producerState.msg);
        } finally {
            coordinationState.producersBlocked.decrementAndGet();
        }
    }

    private void consumerIteration(ConsumerState consumerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        if (control.stopMeasurement) {
            tryCloseConsumer(consumerState);
            Thread.sleep(1);
            return;
        }
        coordinationState.consumersBlocked.incrementAndGet();
        try {
            TickCursor cursor = consumerState.cursor;
            cursor.next();
            RawMessage received = (RawMessage) cursor.getMessage();
            Test_MessageQueueComparison.processPayload(received.data, 0);
        } catch (CursorIsClosedException e) {
            if (!control.stopMeasurement) {
                // If stop flag is not set yet, then it's time to stop right now
                throw e;
            }
        } finally {
            coordinationState.consumersBlocked.decrementAndGet();
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void tryCloseConsumer(ConsumerState consumerState) {
        TickCursor cursor = consumerState.cursor;
        synchronized (cursor) {
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void arbiterIteration(ProducerState producerState, CoordinationState coordinationState, Control control) throws InterruptedException {
        if (coordinationState.arbiterFinished.get()) {
            // Arbiter already done his job
            Thread.sleep(1);
            return;
        }

        if (!control.startMeasurement) {
            // Measurement not started yet
            Thread.sleep(1);
            return;
        }

        // Arbiter waits measurement to end
        while (!control.stopMeasurement) {
            Thread.sleep(1);
        }

        // Make sure consumers had time to close cursors themselves.
        Thread.sleep(20);

        synchronized (coordinationState.consumerCursors) {
            // Close consumers to unlock producers
            for (TickCursor cursor : coordinationState.consumerCursors) {
                synchronized (cursor) {
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                }
            }
        }

        // Try sending messages so blocked consumers can unblock
        /*while (coordinationState.consumersBlocked.get() > 0) {
            producerState.loader.send(producerState.msg);
            Thread.sleep(5);
        }*/
        // Arbiter finished sending messages for blocked consumers

        coordinationState.arbiterFinished.set(true);
    }

    @State(Scope.Group)
    public static class SharedState {

        final LoadingOptions lo = new LoadingOptions(true);
        final SelectionOptions so = new SelectionOptions(true, true);

        TDBRunner tdbRunner;
        DXTickStream stream;

        @Setup
        public synchronized void setup(ConfigState configState) throws Throwable {
            tdbRunner = new ServerRunner(configState.remote, true);
            tdbRunner.startup();

            StreamOptions options = new StreamOptions();
            options.name = "test";
            options.scope = StreamScope.RUNTIME;
            options.bufferOptions = new BufferOptions();
            options.bufferOptions.initialBufferSize = options.bufferOptions.maxBufferSize = configState.queueSizeKb * 1024;
            options.bufferOptions.lossless = configState.lossless;
            options.setFixedType(Messages.BINARY_MESSAGE_DESCRIPTOR);

            System.setProperty(TickStreamImpl.USE_DISRUPTOR_QUEUE_PROPERTY_NAME, Boolean.toString(configState.disruptor));
            stream = tdbRunner.getTickDb().createAnonymousStream(options);
            //System.out.println("Stream created: " + stream.toString());
        }

        @TearDown
        public synchronized void tearDown() throws Exception {
            ((Disposable) stream).close();
            tdbRunner.shutdown();
            //System.out.println("Stream closed: " + stream.toString());
        }
    }


    @State(Scope.Group)
    public static class CoordinationState {
        volatile boolean configured = false;
        volatile CountDownLatch consumersReady;
        volatile CountDownLatch producersAndConsumersFinished;
        final AtomicInteger consumersRemaining = new AtomicInteger(0);
        final AtomicInteger consumersBlocked = new AtomicInteger(0);
        final AtomicInteger producersBlocked = new AtomicInteger(0);
        final AtomicInteger producerInvocationCount = new AtomicInteger(0);
        final List<TickCursor> consumerCursors = new ArrayList<>();
        final AtomicBoolean arbiterFinished = new AtomicBoolean(false);

        @Setup(Level.Iteration)
        public synchronized void setup() {
            configured = false;
            consumersBlocked.set(0);
            arbiterFinished.set(false);
            producerInvocationCount.set(0);
        }

        @TearDown(Level.Iteration)
        public void tearDown() throws Exception {
            configured = false;
            //System.out.println("Producer invocation count: " + producerInvocationCount.get());
        }

        /**
         * Reconfigure counters for this specific test.
         */
        public void init(int producerCount, int consumerCount) {
            if (!configured) {
                consumersReady = new CountDownLatch(consumerCount);
                consumersRemaining.set(consumerCount);
                producersAndConsumersFinished = new CountDownLatch(producerCount + consumerCount);

                configured = true;
            }
        }
    }

    @State(Scope.Thread)
    public static class ConsumerState {
        private TickCursor cursor;
        private CoordinationState coordinationState;

        @SuppressWarnings("SynchronizeOnNonFinalField")
        @Setup(Level.Iteration)
        public void setup(SharedState sharedState, CoordinationState coordinationState) {
            this.coordinationState = coordinationState;
            synchronized (sharedState.stream) {
                cursor = sharedState.stream.createCursor(sharedState.so);
                cursor.addEntity(new ConstantIdentityKey("MSFT"));
            }
            synchronized (coordinationState.consumerCursors) {
                coordinationState.consumerCursors.add(cursor);
            }
        }

        @TearDown(Level.Iteration)
        public void tearDown() throws Exception {
            coordinationState.consumersRemaining.decrementAndGet();
        }
    }

    @State(Scope.Thread)
    public static class ProducerState {
        private final InstrumentMessage msg = createRawMessage("MSFT");
        private TickLoader loader;


        @SuppressWarnings("SynchronizeOnNonFinalField")
        @Setup(Level.Iteration)
        public void setup(SharedState sharedState) throws InterruptedException {
            synchronized (sharedState.stream) {
                loader = sharedState.stream.createLoader(sharedState.lo);
            }
        }

        @TearDown(Level.Iteration)
        public void tearDown() throws Exception {

            loader.close();
        }
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(Test_MessageQueueComparison_JMH.class.getSimpleName())
            //.forks(1)
            //.syncIterations(false)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .result("message_queue_measurement_report.json")
            .resultFormat(ResultFormatType.JSON)
            .build();

        new Runner(opt).run();
        //Main.main(args);
    }

}