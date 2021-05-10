package com.epam.deltix.qsrv.hf.tickdb.tests;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.qsrv.hf.spi.conn.Disconnectable;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TDBServerCmd;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.tests.procs.*;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.Metrics;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.Report;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.TimestampedMetric;
import com.epam.deltix.timebase.messages.service.BinaryMessage;
import com.epam.deltix.util.LangUtil;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestUtils {

    private static final Log LOG = LogFactory.getLog(TestUtils.class);
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    public static ProcessBuilder createProcessBuilder(Class<?> clazz, String... parameters) {
        List<String> command = new LinkedList<>();
        Path deltixJavaHome = Paths.get(System.getenv("DELTIX_JAVA_HOME"));
        Path javaPath;
        if (Util.IS_WINDOWS_OS) {
            javaPath = deltixJavaHome.resolve("bin").resolve("java.exe");
        } else {
            javaPath = deltixJavaHome.resolve("bin").resolve("java");
        }
        command.add(javaPath.toAbsolutePath().toString());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(clazz.getName());
        command.addAll(Arrays.asList(parameters));
        return new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.INHERIT);
    }

    public static ProcessBuilder createProcessBuilder(Class<?> clazz, Map<String, String> jvmArgs, String... parameters) {
        List<String> command = new LinkedList<>();
        command.add("java");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        jvmArgs.forEach((key, value) -> command.add(getProperty(key, value)));
        command.add(clazz.getName());
        command.addAll(Arrays.asList(parameters));
        return new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.INHERIT);
    }

    public static ProcessBuilder createProcessBuilder(Class<?> clazz, String tbHeap, Map<String, String> jvmArgs, String... parameters) {
        List<String> command = new LinkedList<>();
        command.add("java");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("-Xmx" + tbHeap);
        jvmArgs.forEach((key, value) -> command.add(getProperty(key, value)));
        command.add(clazz.getName());
        command.addAll(Arrays.asList(parameters));
        return new ProcessBuilder(command).redirectOutput(ProcessBuilder.Redirect.INHERIT);
    }

    public static ProcessBuilder createProcessBuilder(Class<?> clazz, File outFile, String... parameters) {
        List<String> command = new LinkedList<>();
        command.add("java");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(clazz.getName());
        command.addAll(Arrays.asList(parameters));
        return new ProcessBuilder(command)
                .redirectOutput(outFile)
                .redirectError(outFile);
    }

    private static String getProperty(String key, String value) {
        return String.format("-D%s=%s", key, value);
    }

    public static ProcessBuilder getPurgeProcess(String dbUrl, String streamKey, long purgePeriod, long purgeInterval) {
        return createProcessBuilder(TestPurge.class,
                "-db", dbUrl,
                "-stream", streamKey,
                "-purgePeriod", Long.toString(purgePeriod),
                "-purgeInterval", Long.toString(purgeInterval));
    }

    public static ProcessBuilder getPurgeProcess(String dbUrl, String streamKey, long purgePeriod, long purgeInterval,
                                                 Path logsDir) {
        Path config = createLogConfig(logsDir, streamKey + "-purge");
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("gflog.config", config.toAbsolutePath().toString());
        return createProcessBuilder(TestPurge.class, jvmArgs,
                "-db", dbUrl,
                "-stream", streamKey,
                "-purgePeriod", Long.toString(purgePeriod),
                "-purgeInterval", Long.toString(purgeInterval));
    }


    public static ProcessBuilder getPurgeProcess(String dbUrl, String streamKey, long purgePeriod, long purgeInterval,
                                                 String outFile) {
        String gflogConfig = "gflog-purge.xml";
        createGFLogConfig(Paths.get(outFile).toAbsolutePath().toString(), gflogConfig);
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("gflog.config", gflogConfig);
        return createProcessBuilder(TestPurge.class, jvmArgs,
                "-db", dbUrl,
                "-stream", streamKey,
                "-purgePeriod", Long.toString(purgePeriod),
                "-purgeInterval", Long.toString(purgeInterval));
    }

    public static ProcessBuilder getTruncateProcess(String dbUrl, String streamKey, long truncatePeriod,
                                                    long truncateInterval, int symbols) {
        return createProcessBuilder(TestTruncate.class,
                "-db", dbUrl,
                "-stream", streamKey,
                "-truncatePeriod", Long.toString(truncatePeriod),
                "-truncateInterval", Long.toString(truncateInterval),
                "-symbols", Integer.toString(symbols));
    }

    public static ProcessBuilder getTruncateProcess(String dbUrl, String streamKey, long truncatePeriod,
                                                    long truncateInterval, int symbols, String outFile) {
        String gflogConfig = "gflog-truncate.xml";
        createGFLogConfig(Paths.get(outFile).toAbsolutePath().toString(), gflogConfig);
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("gflog.config", gflogConfig);
        return createProcessBuilder(TestTruncate.class, jvmArgs,
                "-db", dbUrl,
                "-stream", streamKey,
                "-truncatePeriod", Long.toString(truncatePeriod),
                "-truncateInterval", Long.toString(truncateInterval),
                "-symbols", Integer.toString(symbols));
    }

    public static ProcessBuilder getLoadProcess(String dbUrl, String streamKey, int loadRate, int symbols) {

        return createProcessBuilder(TestLoader.class,
                "-db", dbUrl,
                "-stream", streamKey,
                "-rate", Integer.toString(loadRate),
                "-symbols", Integer.toString(symbols));
    }

    public static ProcessBuilder getLoadProcess(String dbUrl, String streamKey, int loadRate, int symbols, String outFile) {
        String gflogConfig = "gflog-loaders.xml";
        createGFLogConfig(Paths.get(outFile).toAbsolutePath().toString(), gflogConfig);
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("gflog.config", gflogConfig);
        return createProcessBuilder(TestLoader.class, jvmArgs,
                "-db", dbUrl,
                "-stream", streamKey,
                "-rate", Integer.toString(loadRate),
                "-symbols", Integer.toString(symbols));
    }

    public static ProcessBuilder getStressLoadProcess(String dbUrl, String streamKey, int loadRate, int symbols,
                                                      String reportsDir, String heap, int payload, boolean deleteSymbol) {
        return createProcessBuilder(StressTestLoader.class, heap, Collections.emptyMap(),
                "-db", dbUrl,
                "-stream", streamKey,
                "-rate", Integer.toString(loadRate),
                "-symbols", Integer.toString(symbols),
                "-reports", reportsDir,
                "-payload", Integer.toString(payload),
                deleteSymbol ? "-deleteSymbol" : "");
    }

    public static ProcessBuilder getStressLoadProcess(String dbUrl, String streamKey, int loadRate, int symbols,
                                                      String reportsDir, String heap, int payload, boolean deleteSymbol,
                                                      Path logsDir) {
        Path config = createLogConfig(logsDir, streamKey + "-loader");
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("gflog.config", config.toAbsolutePath().toString());
        return createProcessBuilder(StressTestLoader.class, heap, jvmArgs,
                "-db", dbUrl,
                "-stream", streamKey,
                "-rate", Integer.toString(loadRate),
                "-symbols", Integer.toString(symbols),
                "-reports", reportsDir,
                "-payload", Integer.toString(payload),
                deleteSymbol ? "-deleteSymbol" : "");
    }

    public static ProcessBuilder getReadProcess(String dbUrl, String streamKey, int readers) {
        return createProcessBuilder(TestReader.class,
                "-db", dbUrl,
                "-stream", streamKey,
                "-readers", Integer.toString(readers));
    }

    public static ProcessBuilder getReadRawProcess(String dbUrl, String streamKey, int readers, String reportsDir,
                                                   String heap, Path logsDir) {
        Path config = createLogConfig(logsDir, streamKey + "-readers");
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("gflog.config", config.toAbsolutePath().toString());
        return createProcessBuilder(TestReader.class, heap, jvmArgs,
                "-db", dbUrl,
                "-stream", streamKey,
                "-readers", Integer.toString(readers),
                "-reports", reportsDir,
                "-raw");
    }

    public static ProcessBuilder getReadRawProcess(String dbUrl, String streamKey, int readers, String reportsDir,
                                                   String heap, int symbols) {
        return createProcessBuilder(TestReader.class, heap, Collections.emptyMap(),
                "-db", dbUrl,
                "-stream", streamKey,
                "-readers", Integer.toString(readers),
                "-reports", reportsDir,
                "-symbols", Integer.toString(symbols),
                "-raw");
    }

    public static ProcessBuilder getReadProcess(String dbUrl, String streamKey, int readers, String outFile) {
        String gflogConfig = "gflog-readers.xml";
        createGFLogConfig(Paths.get(outFile).toAbsolutePath().toString(), gflogConfig);
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("gflog.config", gflogConfig);
        return createProcessBuilder(TestReader.class, jvmArgs,
                "-db", dbUrl,
                "-stream", streamKey,
                "-readers", Integer.toString(readers));
    }

    public static ProcessBuilder getTbProcess(int port, String home, String tbVersion) {
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("TimeBase.version", tbVersion);
        return createProcessBuilder(TDBServerCmd.class, jvmArgs,
                "-home", home,
                "-port", Integer.toString(port),
                "-tb");
    }

    public static ProcessBuilder getTbProcess(int port, String home, String tbVersion, String tbHeap, boolean withoutTomcat) {
        return withoutTomcat ? getTDBProcess(port, home, tbVersion, tbHeap) : getTbProcess(port, home, tbVersion, tbHeap);
    }

    public static ProcessBuilder getTbProcess(int port, String home, String tbVersion, String tbHeap) {
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("TimeBase.version", tbVersion);
        return createProcessBuilder(TDBServerCmd.class, tbHeap, jvmArgs,
                "-home", home,
                "-port", Integer.toString(port),
                "-tb");
    }

    public static ProcessBuilder getTDBProcess(int port, String home, String tbVersion, String tbHeap) {
        Map<String, String> jvmArgs = new HashMap<>();
        jvmArgs.put("TimeBase.version", tbVersion);
        jvmArgs.put("deltix.qsrv.home", home);
        return createProcessBuilder(TDBServerCmd.class, tbHeap, jvmArgs,
                "-db", home + File.separator + (tbVersion.equals("5.0") ? "timebase" : "tickdb"),
                "-port", Integer.toString(port),
                "-tb");
    }

    public static <T> T[] pick(T[] array, int elements, IntFunction<T[]> generator) {
        if (elements > array.length)
            return array;

        List<Integer> indices = IntStream.range(0, array.length).boxed().collect(Collectors.toList());
        Collections.shuffle(indices);

        T[] result = generator.apply(elements);
        for (int i = 0; i < elements; i++) {
            result[i] = array[indices.get(i)];
        }

        return result;
    }

    public static IdentityKey[] selectRandomSymbols(DXTickStream stream, int symbols) {
        return pick(stream.listEntities(), symbols, IdentityKey[]::new);
    }

    public static void exitGracefully(Process process) {
        try {
            if (process != null) {
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                    writer.write("Hello!\n");
                }
                process.waitFor(10, TimeUnit.SECONDS);
            }
        } catch (Throwable th) {
            LOG.error().append("Couldn't exit process gracefully, will destroy it: ")
                    .append(process)
                    .append(": ")
                    .append(th).commit();
            destroy(process);
        }
    }

    public static void destroy(Process process) {
        try {
            if (process != null) {
                process.destroy();
            }
        } catch (Throwable th) {
            LOG.error().append("Error while destroying process ")
                    .append(process)
                    .append(": ")
                    .append(th).commit();
        }
    }

    public static void destroyForcibly(Process process) {
        try {
            if (process != null) {
                Process toBeDestroyed = process.destroyForcibly();
                do {
                    Thread.sleep(1000);
                } while (toBeDestroyed.isAlive());
            }
        } catch (Throwable th) {
            LOG.error().append("Error while destroying process ")
                    .append(process)
                    .append(": ")
                    .append(th).commit();
        }
    }

    public static DXTickStream createUniversalMarketStream(DXTickDB db, String streamKey) {
        StreamOptions streamOptions = new StreamOptions(StreamScope.DURABLE, streamKey, streamKey, 0);
        RecordClassDescriptor[] rcds = StreamConfigurationHelper.mkUniversalMarketDescriptors();
        streamOptions.setPolymorphic(rcds);

        DXTickStream stream = db.getStream(streamKey);
        if (stream != null)
            stream.delete();

        return db.createStream(streamKey, streamOptions);
    }

    public static DXTickStream createBinaryStream(DXTickDB db, String streamKey, int distributionFactor) {
        StreamOptions streamOptions = new StreamOptions(StreamScope.DURABLE, streamKey, streamKey, distributionFactor);
        Introspector introspector = Introspector.createEmptyMessageIntrospector();
        RecordClassDescriptor rcd;
        try {
            rcd = introspector.introspectRecordClass(BinaryMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
        streamOptions.setFixedType(rcd);

        DXTickStream stream = db.getStream(streamKey);
        if (stream != null)
            stream.delete();

        return db.createStream(streamKey, streamOptions);
    }

    public static DXTickStream createBinaryStream(String dbUrl, String streamKey, int distributionFactor) {
        try (DBWrapper wrapper = new DBWrapper(dbUrl)) {
            return createBinaryStream(wrapper.getDB(), streamKey, distributionFactor);
        }
    }

    public static void createUniversalMarketStream(String dbUrl, String streamKey) {
        try (DXTickDB db = TickDBFactory.openFromUrl(dbUrl, false)) {
            createUniversalMarketStream(db, streamKey);
        }
    }

    public static void addDisconnectListener(DXTickDB db, DisconnectEventListener listener) {
        ((Disconnectable) db).addDisconnectEventListener(listener);
    }

    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz".toCharArray();

    public static String[] getRandomStrings(int n) {
        HashSet<String> strings = new HashSet<>(n);
        while (strings.size() < n) {
            strings.add(getRandomString(8));
        }
        return strings.toArray(new String[n]);
    }

    public static ArrayList<String> getRandomStringsList(int n) {
        HashSet<String> strings = new HashSet<>(n);
        while (strings.size() < n) {
            strings.add(getRandomString(8));
        }
        return new ArrayList<>(strings);
    }

    public static String getRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS[RANDOM.nextInt(CHARS.length)]);
        }
        return sb.toString();
    }

    public static class DBWrapper implements Closeable, AutoCloseable {

        private String dbUrl;

        private volatile DXTickDB db;
        public DBWrapper(String dbUrl) {
            this.dbUrl = dbUrl;
        }

        public synchronized DXTickDB getDB() {
            if (db == null || !db.isOpen()) {
                Util.close(db);
                db = TickDBFactory.openFromUrl(dbUrl, false, 60000);
                addDisconnectListener(db, new DisconnectEventListener() {
                    @Override
                    public void onDisconnected() {
                        Util.close(db);
                        db = null;
                    }

                    @Override
                    public void onReconnected() {
                    }
                });
            }
            return db;
        }

        @Override
        public void close() {
            Util.close(db);
        }

    }
    public static class RandomScheduledTask extends TimerTask {

        private static final Timer TIMER = new Timer();

        private static final Random RANDOM = new Random(System.currentTimeMillis());
        private final Runnable task;
        private final int minDelay;
        private final int maxDelay;
        /**
         * Creates task, that will be scheduled with random delays.
         *
         * @param task     task to run
         * @param minDelay min delay in seconds
         * @param maxDelay max delay in seconds
         */
        public RandomScheduledTask(Runnable task, int minDelay, int maxDelay) {
            this.task = task;
            this.minDelay = minDelay;
            this.maxDelay = maxDelay;
        }

        @Override
        public void run() {
            int delay = (minDelay + RANDOM.nextInt(maxDelay - minDelay)) * 1000;
            TIMER.schedule(new RandomScheduledTask(task, minDelay, maxDelay), delay);
            task.run();
        }

    }

    public static class MessagesMonitor {

        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        private final AtomicLong count = new AtomicLong();
        private final long interval;
        private final Runnable task;
        private final TimestampedMetric<Double> metric = Metrics.createDoubleTimestamped();
        private Future<?> scheduled = null;

        public MessagesMonitor(long interval, String id) {
            this.interval = interval;
            this.task = () -> {
                long messages = count.get();
                count.set(0);
                double value = 1000. * messages / interval;
                metric.addValue(System.currentTimeMillis(), value);
                LOG.info().append(id).append(": ")
                        .append(value)
                        .append(" msg/sec.")
                        .commit();
            };
        }

        public TimestampedMetric<Double> getMetric() {
            return metric;
        }

        public synchronized void start() {
            if (scheduled != null) {
                stop();
            }
            scheduled = executor.scheduleAtFixedRate(task, interval, interval, TimeUnit.MILLISECONDS);
        }

        public synchronized void stop() {
            if (scheduled != null) {
                scheduled.cancel(true);
                scheduled = null;
            }
        }

        public void count() {
            count.incrementAndGet();
        }

    }
    public static File createGFLogConfig(String logPath, String configPath) {
        File file = new File(configPath);
        if (file.exists()) {
            IOUtil.deleteUnchecked(file);
        }
        String config = getGFLogConfig(logPath);
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(config);
        } catch (FileNotFoundException e) {
            LangUtil.rethrowUnchecked(e);
        }
        return file;
    }

    public static Path createLogConfig(Path logsDir, String id) {
        Path path;
        try {
            path = Files.createTempFile(id, ".xml");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        File file = path.toFile();
        if (file.exists()) {
            IOUtil.deleteUnchecked(file);
        }
        String config = getGFLogConfig(logsDir.resolve(id + ".log").toAbsolutePath().toString());
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.write(config);
        } catch (FileNotFoundException e) {
            LangUtil.rethrowUnchecked(e);
        }
        return path;
    }

    private static String getGFLogTemplate() {
        String resourcePath = TestUtils.class.getPackage().getName().replace('.', '/') + "/gflog-template.xml";
        InputStream iStream = TestUtils.class.getClassLoader().getResourceAsStream(resourcePath);
        assert iStream != null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(iStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Cannot access resource " + resourcePath);
    }

    private static String getGFLogConfig(String path) {
        return String.format(getGFLogTemplate(), path);
    }

    public static void prepareReports(Path reportsPath, boolean cleanReports) throws IOException {
        createReportsDirIfNeeded(reportsPath);
        if (cleanReports) {
            Files.walk(reportsPath, 1).forEach(file -> {
                if (!file.toFile().isDirectory() && file.getFileName().toString().endsWith(".json")) {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        LOG.error().append("Could not delete file ")
                                .append(file.getFileName())
                                .append(": ").append(e)
                                .commit();
                    }
                }
            });
        }
    }

    public static void prepareLogs(Path logs, boolean cleanLogs) throws IOException {
        if (!logs.toFile().exists()) {
            Files.createDirectories(logs);
        }
        if (cleanLogs) {
            Files.walk(logs, 1).forEach(file -> {
                if (!file.toFile().isDirectory() && file.getFileName().toString().endsWith(".log")) {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        LOG.error().append("Could not delete file ")
                                .append(file.getFileName())
                                .append(": ").append(e)
                                .commit();
                    }
                }
            });
        }
    }

    private static void createReportsDirIfNeeded(Path reportsPath) throws IOException {
        if (!reportsPath.toFile().exists()) {
            Files.createDirectories(reportsPath);
        }
    }

    public static void scheduleReportFlush(Report report, Path path) {
        Executors.newScheduledThreadPool(1, runnable -> {
            Thread thread = Executors.defaultThreadFactory().newThread(runnable);
            thread.setDaemon(true);
            return thread;
        }).scheduleAtFixedRate(() -> {
            report.writeToFile(path);
            LOG.info().append("Flushed ").append(path.toString()).commit();
        }, 30, 30, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            report.writeToFile(path);
            LOG.info().append("Flushed on shutdown ").append(path).commit();
        }));
    }

    public static void scheduleAction(Runnable runnable, long delay, TimeUnit unit) {
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            return thread;
        }).schedule(runnable, delay, unit);
    }

    public static void main(String[] args) {
        System.out.println(getGFLogConfig("hej.log"));
    }
}
