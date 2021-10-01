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
package com.epam.deltix.qsrv.dtb.fs.azure;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.dtb.fs.azure2.AzureFsBase;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.os.CommonSysProps;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Measures performance of reading from Azure DataLake using {@link AzureFS}
 * with different values of {@link AzureFsBase#AZURE_REOPEN_ON_SEEK_THRESHOLD_PROP} parameter.
 *
 * @author Alexei Osipov
 */
@Warmup(time = 10, timeUnit = TimeUnit.SECONDS, iterations = 0)
@Measurement(time = 2, timeUnit = TimeUnit.MINUTES, iterations = 1)
@BenchmarkMode(Mode.Throughput)
@Fork(1)
@Threads(1)
public class ReopenThresholdBenchmark {
    public static final String SELECTED_SYMBOLS_PATH = "ticks_selected.txt";

    private static final Properties benchProps = getProps();

    private static Properties getProps() {
        Properties props = new Properties();
        try (FileInputStream is = new FileInputStream("cursor_read_bench.properties")){
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return props;
    }

    @Benchmark
    @OperationsPerInvocation(100_000)
    public long bench(SparseStreamState state) throws Exception {
        return readMessages(state.cursor, 100_000);
    }


    @State(Scope.Thread)
    public static class SparseStreamState extends StreamState {
        @Setup
        public void prepare(DbState dbState) throws Exception {
            cursor = setupCursor(dbState, benchProps.getProperty("sparseStream").trim());
        }

    }


    @State(Scope.Benchmark)
    public static class DbState {
        //@Param({"1", "4", "8", "16", "32", "64", "128"})
        @Param({"8", "64", "256", "1024", "2048", 8 * 1024 + ""})
        long reopenThresholdKb;

        DXTickDB db;

        @Setup
        public void prepare() throws Exception {
            System.setProperty(AzureFsBase.AZURE_REOPEN_ON_SEEK_THRESHOLD_PROP, reopenThresholdKb * 1024 + "");
            db = constructDB();
        }

        @TearDown
        public void teardown() {
            db.close();
            QuickExecutor.shutdownGlobalInstance();
        }
    }

    public static abstract class StreamState {
        TickCursor cursor;

        protected TickCursor setupCursor(DbState dbState, String name) throws IOException {
            final DXTickStream stream = dbState.db.getStream(name);
            TickCursor cursor = stream.createCursor(new SelectionOptions());

            List<String> selectedSymbols = getSelectedSymbols();
            for (String symbol : selectedSymbols) {
                cursor.addEntity(new ConstantIdentityKey(symbol));
            }
            cursor.reset(0);
            return cursor;
        }

        private static List<String> getSelectedSymbols() throws IOException {
            String content = new String(Files.readAllBytes(Paths.get(SELECTED_SYMBOLS_PATH)));
            return Lists.newArrayList(Splitter.on("\n").trimResults().omitEmptyStrings().split(content));
        }
    }

    public static DXTickDB constructDB() throws Exception {
        // Load benchmark properties
        String home = benchProps.getProperty("deltix.home").trim();
        String qsHome = benchProps.getProperty("qsHome").trim();

        // Setub TB sustem props
        Home.set(home);
        CommonSysProps.mergeProps();

        String timebaseLocation = qsHome + "\\timebase";
        File folder = new File(timebaseLocation);

        QSHome.set(folder.getParent());

        final DXTickDB db = new TickDBImpl(folder);
        db.open(false);
        return db;

        //final DXTickStream stream = db.getStream(STREAM_KEY);
    }

    private long readMessages(TickCursor cursor, int n) {
        long timeStampMs = 0;
        for (int i = 0; i < n; i++) {
            if (cursor.next()) {
                InstrumentMessage message = cursor.getMessage();
                timeStampMs = message.getTimeStampMs();
            } else {
                System.out.println("Cursor has no more messages. Resetting...");
                cursor.reset(0);
            }
        }
        return timeStampMs;
    }




    public static void main(String[] args) throws RunnerException {
        String simpleName = ReopenThresholdBenchmark.class.getSimpleName();
        Options opt = new OptionsBuilder()
                .include(simpleName)
                //.forks(1)
                //.syncIterations(false)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                .result(simpleName + "_report.json")
                .resultFormat(ResultFormatType.JSON)
                .build();

        Collection<RunResult> runResults = new Runner(opt).run();
        System.out.println(runResults);
    }
}