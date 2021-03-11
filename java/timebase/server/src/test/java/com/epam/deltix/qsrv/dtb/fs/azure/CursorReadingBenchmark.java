package com.epam.deltix.qsrv.dtb.fs.azure;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.os.CommonSysProps;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Measures performance of reading from Azure DataLake using {@link AzureFS}
 * with differently stored streams.
 *
 * Note: stream configuration is not part of this benchmark class. It provided externally via cursor_read_bench.properties.
 *
 * @author Alexei Osipov
 */
@Warmup(time = 10, timeUnit = TimeUnit.SECONDS, iterations = 8)
@Measurement(time = 1, timeUnit = TimeUnit.MINUTES, iterations = 3)
@BenchmarkMode(Mode.Throughput)
@Fork(0)
@Threads(1)
public class CursorReadingBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkSettings {
        public static final String javaMemory = "10G";
        public final long ramDiskSize = 9737418240L;
        public final boolean useOnlySelectedEntities = false; // If true, symbols from ticks_selected.txt will be used. Otherwise we read all symbols.
    }

    // This file may contain list of symbols to use from stream
    public static final String SELECTED_SYMBOLS_PATH = "ticks_selected.txt";

    private static final Properties benchProps = getProps();

    private static Properties getProps() {
        Properties props = new Properties();
        try (FileInputStream is = new FileInputStream("CursorReadingBenchmark.properties")){
            props.load(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return props;
    }

    /**
     * Sparse data: Steam in Azure DL with many symbols.
     */
    //@Benchmark
    @OperationsPerInvocation(100_000)
    public long sparseData(SparseStreamState state) throws Exception {
        return readMessages(state.cursor, 100_000);
    }

    /**
     * Condensed data: Steam in Azure DL with only needed symbols. So there is no symbols to skip.
     */
    //@Benchmark
    @OperationsPerInvocation(100_000)
    public long condensedData(CondensedStreamState state) throws Exception {
        return readMessages(state.cursor, 100_000);
    }

    /**
     * Local data: Steam with data on local disk.
     */
    @Benchmark
    @OperationsPerInvocation(10_000)
    public long localData(LocalStreamState state) throws Exception {
        return readMessages(state.cursor, 10_000);
    }

    @Benchmark
    @OperationsPerInvocation(10_000)
    public long localDataWithLive(LocalStreamWithLiveState state) throws Exception {
        return readMessages(state.cursor, 10_000);
    }

    @Benchmark
    @OperationsPerInvocation(10_000)
    public long localDataWithTempLive(LocalStreamWithTempLiveState state) throws Exception {
        return readMessages(state.cursor, 10_000);
    }

    //@Benchmark
    @OperationsPerInvocation(10_000)
    public long localFixedData(LocalFixedStreamState state) throws Exception {
        return readMessages(state.cursor, 10_000);
    }

    @State(Scope.Thread)
    public static class SparseStreamState extends StreamState {
        @Setup
        public void prepare(BenchmarkSettings bs, DbState dbState) throws Exception {
            cursor = setupCursor(bs, dbState, getProp("sparseStream").trim(), false, false);
        }

    }

    @State(Scope.Thread)
    public static class CondensedStreamState extends StreamState {
        @Setup
        public void prepare(BenchmarkSettings bs, DbState dbState) throws Exception {
            cursor = setupCursor(bs, dbState, getProp("condensedStream").trim(), false, false);
        }
    }

    @State(Scope.Thread)
    public static class LocalStreamState extends StreamState {
        @Setup
        public void prepare(BenchmarkSettings bs, DbState dbState) throws Exception {
            cursor = setupCursor(bs, dbState, getProp("localStream").trim(), false, false);
        }
    }

    @State(Scope.Thread)
    public static class LocalStreamWithLiveState extends StreamState {
        @Setup
        public void prepare(BenchmarkSettings bs, DbState dbState) throws Exception {
            cursor = setupCursor(bs, dbState, getProp("localStream").trim(), false, true);
            /*
            cursor = setupCursor(bs, dbState, getProp("localStream").trim(), false, false);
            TickCursor liveCursor = setupCursor(bs, dbState, getProp("localStream").trim(), false, true);
            readMessages(liveCursor, 100_000);
            liveCursor.close();
            */
        }
    }

    @State(Scope.Thread)
    public static class LocalStreamWithTempLiveState extends StreamState {
        @Setup
        public void prepare(BenchmarkSettings bs, DbState dbState) throws Exception {
            cursor = setupCursor(bs, dbState, getProp("localStream").trim(), false, false);

            TickCursor liveCursor = setupCursor(bs, dbState, getProp("localStream").trim(), false, true);
            readMessages(liveCursor, 100_000);
            liveCursor.close();
        }
    }

    @State(Scope.Thread)
    public static class LocalFixedStreamState extends StreamState {
        @Setup
        public void prepare(BenchmarkSettings bs, DbState dbState) throws Exception {
            cursor = setupCursor(bs, dbState, getProp("localStream").trim(), true, false);
        }
    }

    @State(Scope.Benchmark)
    public static class DbState {
        DXTickDB db;

        @Setup
        public void prepare(BenchmarkSettings bs) throws Exception {
            db = constructDB(bs.ramDiskSize);
        }

        @TearDown
        public void teardown() {
            db.close();
            QuickExecutor.shutdownGlobalInstance();
        }
    }

    public static abstract class StreamState {
        TickCursor cursor;

        protected TickCursor setupCursor(BenchmarkSettings bs, DbState dbState, String name, boolean fixed, boolean live) throws IOException {
            name = name.trim();
            final DXTickStream stream = dbState.db.getStream(name);
            IdentityKey[] entities = bs.useOnlySelectedEntities ? getSeletedEntitites() : null;
            SelectionOptions options = new SelectionOptions();
            options.restrictStreamType = fixed;
            options.live = live;
            options.realTimeNotification = live;
            TickCursor select = stream.select(Long.MIN_VALUE, options, null, entities);
            if (live) {
                select.setAvailabilityListener(new Runnable() {
                    @Override
                    public void run() {
                        // Do nothing.
                    }
                });
            }
            return Preconditions.checkNotNull(select, "%s stream does not exist", name);
        }

        @Nonnull
        private IdentityKey[] getSeletedEntitites() throws IOException {
            List<String> selectedSymbols = getSelectedSymbols();
            List<IdentityKey> entities = new ArrayList<>();
            for (String symbol : selectedSymbols) {
                entities.add(new ConstantIdentityKey(symbol));
            }

            return entities.toArray(new IdentityKey[0]);
        }

        private static List<String> getSelectedSymbols() throws IOException {
            String content = new String(Files.readAllBytes(Paths.get(SELECTED_SYMBOLS_PATH)));
            return Lists.newArrayList(Splitter.on("\n").trimResults().omitEmptyStrings().split(content));
        }

        protected String getProp(String prop) {
            return Preconditions.checkNotNull(benchProps.getProperty(prop), "property \"%s\" is not set", prop);
        }

        @TearDown
        public void teardown() {
            cursor.close();
        }
    }

    public static DXTickDB constructDB(long ramdisksize) throws Exception {
        // Load benchmark properties
        String home = benchProps.getProperty("deltix.home").trim();
        String qsHome = benchProps.getProperty("qsHome").trim();

        // Setub TB sustem props
        Home.set(home);
        CommonSysProps.mergeProps();

        String timebaseLocation = qsHome + "\\timebase";
        File folder = new File(timebaseLocation);

        QSHome.set(folder.getParent());

        DataCacheOptions options = new DataCacheOptions (Integer.MAX_VALUE, ramdisksize);
        final DXTickDB db = new TickDBImpl(options, folder);
        db.open(false);
        return db;

        //final DXTickStream stream = db.getStream(STREAM_KEY);
    }

    private static long readMessages(TickCursor cursor, int n) {
        long timeStampMs = 0;
        for (int i = 0; i < n; i++) {

            boolean next;
            try {
                next = cursor.next();
            } catch (UnavailableResourceException e) {
                next = false;
            }

            if (next) {
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
        String simpleName = CursorReadingBenchmark.class.getSimpleName();
        Options opt = new OptionsBuilder()
                .include(simpleName)
                .jvmArgs("-Xms" + BenchmarkSettings.javaMemory, "-Xmx" + BenchmarkSettings.javaMemory)
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
