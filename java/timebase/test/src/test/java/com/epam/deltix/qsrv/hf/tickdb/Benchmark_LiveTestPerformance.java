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
package com.epam.deltix.qsrv.hf.tickdb;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(0)
@Warmup(time = 15, timeUnit = TimeUnit.SECONDS, iterations = 1)
@Measurement(time = 30, timeUnit = TimeUnit.SECONDS, iterations = 5)
public class Benchmark_LiveTestPerformance {

    private static final RecordClassDescriptor descriptor = createDescriptor();

    @Benchmark
    @GroupThreads(3) // Controls number of reader threads
    @Group("testThroughputP1C1")
    public InstrumentMessage measureConsumerP1C1(ConsumerState consumerState) {
        return consumerIteration(consumerState);
    }

    @Benchmark
    @GroupThreads(1)
    @Group("testThroughputP1C1")
    public void measureProducerP1C1(ProducerState producerState) {
        producerIteration(producerState);
    }

    private void producerIteration(ProducerState producerState) {
        while (true) {
            if (producerState.intervalBetweenMessagesInNanos != 0) {
                if (System.nanoTime() < producerState.nextNanoTime)
                    continue; // spin-wait
            }

            String[] symbols = producerState.symbols;
            producerState.trade.setSymbol(symbols[(int)(producerState.count % symbols.length)]);
            //trade.setNanoTime(System.nanoTime());
            //trade.timestamp = System.nanoTime();
            producerState.loader.send (producerState.trade);

            producerState.nextNanoTime += producerState.intervalBetweenMessagesInNanos;
            producerState.count++;
            break;
        }
    }

    private InstrumentMessage consumerIteration(ConsumerState consumerState) {
        consumerState.cursor.next();
        return consumerState.cursor.getMessage();
    }


    @State(Scope.Group)
    public static class SharedState {

        String          tb = "dxtick://localhost:8011";
        int             throughput = 1000_000;
        int             messageSize = 100;
        int             df = 0;
        int             symbols = 1000;

        final LoadingOptions lo = new LoadingOptions(true);
        final SelectionOptions so = new SelectionOptions(true, true);

        DXTickDB db;
        DXTickStream stream;
        String[] names;

        @Setup(Level.Trial)
        public synchronized void setup() {
            //System.out.println("SharedState.setup");
            StringBuffer ch = new StringBuffer("AAAAAAAA");
            this.names = new String[symbols];
            for (int i = 0; i < names.length; i++) {
                names[i] = ch.toString();
                increment(ch, 4);
            }


            this.db = TickDBFactory.createFromUrl(tb);
            db.open(false);
        }

        @Setup(Level.Iteration)
        public synchronized void setupStream() {
            String id = UUID.randomUUID().toString();
            this.stream = db.createStream(id, StreamOptions.fixedType(StreamScope.TRANSIENT, id, id, df, descriptor));
        }

        @TearDown(Level.Iteration)
        public synchronized void tearDownStream() {
            stream.delete();
        }

        @TearDown(Level.Trial)
        public synchronized void tearDown() {
            db.close();
        }

        private void increment(StringBuffer symbol, int index) {
            if (symbol.charAt(index) == (int)'Z') {
                increment(symbol, index - 1);
                symbol.setCharAt(index, 'A');
            }
            else
                symbol.setCharAt(index, (char) (symbol.charAt(index) + 1));
        }
    }

    @State(Scope.Thread)
    public static class ConsumerState {
        private TickCursor cursor;

        @Setup(Level.Iteration)
        public void setup(SharedState sharedState) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (sharedState) {
                cursor = sharedState.stream.select(0, sharedState.so);
            }
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            try {
                cursor.close();
            } catch (Exception ignore) {
            }
        }
    }

    @State(Scope.Thread)
    public static class ProducerState {
        final RawMessage trade = new RawMessage();
        private TickLoader loader;

        private String[] symbols;

        long intervalBetweenMessagesInNanos;
        long nextNanoTime;
        long count = 0;

        @Setup(Level.Iteration)
        public void setup(SharedState sharedState) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (sharedState) {
                loader = sharedState.stream.createLoader(sharedState.lo);
            }
            intervalBetweenMessagesInNanos = TimeUnit.SECONDS.toNanos(1) / sharedState.throughput;
            nextNanoTime = System.nanoTime() + intervalBetweenMessagesInNanos;
            symbols = sharedState.names;

            trade.type = descriptor;
            trade.data = new byte[sharedState.messageSize];
            trade.setSymbol("DLTX");
        }

        @TearDown(Level.Iteration)
        public void tearDown() {
            try {
                loader.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static RecordClassDescriptor createDescriptor() {
        RecordClassDescriptor mcd = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        return StreamConfigurationHelper.mkTradeMessageDescriptor(
                mcd, null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(Benchmark_LiveTestPerformance.class.getSimpleName())
                //.measurementIterations(5)
                //.syncIterations(false)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .result("Benchmark_LiveTestPerformance.json")
                .resultFormat(ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
        //Main.main(args);
    }
}