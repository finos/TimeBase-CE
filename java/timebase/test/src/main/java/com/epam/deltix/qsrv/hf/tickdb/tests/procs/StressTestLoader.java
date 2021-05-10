package com.epam.deltix.qsrv.hf.tickdb.tests.procs;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.tests.ShutdownSignal;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.hf.tickdb.tests.RandomBinaryMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.DBWrapper;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.MessagesMonitor;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.JsonReport;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.Report;
import com.epam.deltix.util.cmdline.DefaultApplication;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StressTestLoader extends DefaultApplication implements Runnable {

    private final static Log LOG = LogFactory.getLog(StressTestLoader.class);

    private final String key;
    private final String dbUrl;
    private final int maxRate;
    private final int symbols;
    private final boolean deleteSymbol;
    private final RandomBinaryMessageSource messageSource;
    private final RateLimiterRegistry registry;
    private final DBWrapper dbWrapper;
    private final String reportsDir;
    private final String id;
    private final ShutdownSignal shutdownSignal = new ShutdownSignal();

    protected StressTestLoader(String[] args) {
        super(args);
        deleteSymbol = isArgSpecified("-deleteSymbol");
        key = getArgValue("-stream", "loadTestStream");
        dbUrl = getArgValue("-db", "dxtick://localhost:8011");
        maxRate = getIntArgValue("-rate", 100000);
        symbols = getIntArgValue("-symbols", 100);
        int payload = getIntArgValue("-payload", 100);
        messageSource = new RandomBinaryMessageSource(payload, symbols);
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(maxRate)
                .build();
        registry = RateLimiterRegistry.of(config);
        dbWrapper = new DBWrapper(dbUrl);
        reportsDir = getArgValue("-reports", null);
        id = "Loader-" + key;
    }

    private class StressLoaderTask implements Runnable {

        @Override
        public void run() {
            LOG.info().append("Starting load task to stream ").append(key)
                    .append(" on ").append(dbUrl)
                    .append(" with ").append(symbols).append(" symbols with max rate ")
                    .append(maxRate)
                    .commit();
            MessagesMonitor messagesMonitor = new MessagesMonitor(30000, id);
            if (reportsDir != null) {
                Report report = new JsonReport();
                report.addMetric("messagesRate", messagesMonitor.getMetric());
                TestUtils.scheduleReportFlush(report, Paths.get(reportsDir, id + ".json"));
            }
            try {
                messagesMonitor.start();
                if (deleteSymbol) {
                    LOG.info().append("Scheduling symbol delete in 30 seconds").commit();
                    TestUtils.scheduleAction(this::deleteSymbol, 30, TimeUnit.SECONDS);
                }
                while (!shutdownSignal.isSignaled()) {
                    try {
                        runUnchecked(messagesMonitor);
                    } catch (Exception exc) {
                        LOG.error().append(exc).commit();
                    }
                }
            } finally {
                LOG.info().append("Exiting load process ").append(id).commit();
                System.out.println("Exiting load process " + id);
                messagesMonitor.stop();
                dbWrapper.close();
            }
        }

        private DXTickStream getStream() {
            DXTickStream stream;
            long startTime = System.currentTimeMillis();
            long interval;
            do {
                stream = dbWrapper.getDB().getStream(key);
                interval = System.currentTimeMillis() - startTime;
            } while (stream == null && interval < 20000);
            if (stream == null) {
                throw new RuntimeException("Timeout while waiting for stream " + key);
            }
            return stream;
        }

        private void deleteSymbol() {
            DXTickStream stream = getStream();
            IdentityKey[] ids = stream.listEntities();
            String symbol = messageSource.removeSymbol();
            IdentityKey identity = null;
            for (int i = 0; i < ids.length; i++) {
                if (symbol.equals(ids[i].getSymbol().toString())) {
                    identity = ids[i];
                    break;
                }
            }
            if (identity == null) {
                throw new RuntimeException("Symbol " + symbol + " not found in stream " + key);
            }
            LOG.info().append("Stop loading identity ").append(identity).commit();

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            LOG.info().append("Truncating identity ").append(identity).commit();
            stream.truncate(0, identity);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            LOG.info().append("Deleting identity ").append(identity).commit();
            stream.clear(identity);
//            stream.delete(TimeStamp.fromMilliseconds(0), TimeStamp.fromMilliseconds(System.currentTimeMillis() + 1000), identity);
        }

        private void runUnchecked(MessagesMonitor monitor) {
            RateLimiter rateLimiter = registry.rateLimiter("RateLimiter-" + id);
            DXTickStream stream = getStream();
            try (TickLoader loader = stream.createLoader()) {
                while (messageSource.next() && !shutdownSignal.isSignaled()) {
                    if (rateLimiter.acquirePermission()) {
                        loader.send(messageSource.getMessage());
                        monitor.count();
                    }
                }
            }
        }

    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new StressLoaderTask());
        new Scanner(System.in).nextLine();
        shutdownSignal.signal();
        System.exit(0);
    }

    public static void main(String[] args) {
        new StressTestLoader(args).start();
    }
}
