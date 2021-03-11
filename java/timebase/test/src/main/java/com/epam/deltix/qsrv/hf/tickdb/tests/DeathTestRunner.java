package com.epam.deltix.qsrv.hf.tickdb.tests;

import com.epam.deltix.anvil.util.ShutdownSignal;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.util.cmdline.DefaultApplication;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.*;

public class DeathTestRunner extends DefaultApplication {

    private static final Log LOG = LogFactory.getLog(DeathTestRunner.class);

    protected DeathTestRunner(String[] args) {
        super(args);
    }

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private Process tbProcess;
    private Process purgeProcess;
    private Process loadProcess;
    private Process readProcess;
    private Process truncateProcess;

    @Override
    protected void run() throws Throwable {
        boolean filesLog = isArgSpecified("-filesLog");
        int readers = getIntArgValue("-readers", 10);
        int loadRate = getIntArgValue("-loadRate", 100000);
        int symbols = getIntArgValue("-loadSymbols", 100);
        long purgePeriod = getLongArgValue("-purgePeriod", 8 * 60 * 1000);
        long purgeInterval = getLongArgValue("-purgeInterval", 10 * 60 * 1000);
        long truncatePeriod = getLongArgValue("-truncatePeriod", 2 * 60 * 1000);
        long truncateInterval = getLongArgValue("-truncateInterval", 10 * 60 * 1000);
        int truncateSymbols = getIntArgValue("-truncateSymbols", 10);
        String streamKey = getArgValue("-stream", "testStream");
        int port = getIntArgValue("-port", 8011);
        String tbVersion = getArgValue("-tbVersion", "5.0");
        String home = getMandatoryArgValue("-home");

        String dbUrl = String.format("dxtick://localhost:%d", port);

        ProcessBuilder purgeProcessBuilder;
        ProcessBuilder loadProcessBuilder;
        ProcessBuilder readProcessBuilder;
        ProcessBuilder truncateProcessBuilder;
        ProcessBuilder tbProcessBuilder = getTbProcess(port, home, tbVersion);
        if (filesLog) {
            String purgeLog = getArgValue("-purgeLog", "purge.log");
            String truncateLog = getArgValue("-truncateLog", "truncate.log");
            String readersLog = getArgValue("-readLog", "readers.log");
            String loadersLog = getArgValue("-loadLog", "loaders.log");
            purgeProcessBuilder = getPurgeProcess(dbUrl, streamKey, purgePeriod, purgeInterval, purgeLog);
            loadProcessBuilder = getLoadProcess(dbUrl, streamKey, loadRate, symbols, loadersLog);
            readProcessBuilder = getReadProcess(dbUrl, streamKey, readers, readersLog);
            truncateProcessBuilder = getTruncateProcess(dbUrl, streamKey, truncatePeriod, truncateInterval,
                    truncateSymbols, truncateLog);
        } else {
            purgeProcessBuilder = getPurgeProcess(dbUrl, streamKey, purgePeriod, purgeInterval);
            loadProcessBuilder = getLoadProcess(dbUrl, streamKey, loadRate, symbols);
            readProcessBuilder = getReadProcess(dbUrl, streamKey, readers);
            truncateProcessBuilder = getTruncateProcess(dbUrl, streamKey, truncatePeriod, truncateInterval, truncateSymbols);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info().append("Destroying all processes.").commit();
            destroy(purgeProcess);
            destroy(truncateProcess);
            destroy(loadProcess);
            destroy(readProcess);
            destroy(tbProcess);
        }));
        ShutdownSignal shutdownSignal = new ShutdownSignal();

        try {
            tbProcess = tbProcessBuilder.start();
            try (DBWrapper wrapper = new DBWrapper(dbUrl)) {
                createUniversalMarketStream(wrapper.getDB(), streamKey);
            }
            loadProcess = loadProcessBuilder.start();
//            readProcess = readProcessBuilder.start();
//            truncateProcess = truncateProcessBuilder.start();
//            purgeProcess = purgeProcessBuilder.start();
            Runnable destroyTask = () -> {
                LOG.warn().append("Destroying timebase process!").commit();
                destroyForcibly(tbProcess);
                try {
                    setTbProcess(tbProcessBuilder.start());
                } catch (IOException e) {
                    LOG.error().append(e).commit();
                    shutdownSignal.signal();
                }
            };
            Thread.sleep(30000);
            executor.execute(new RandomScheduledTask(destroyTask, 20, 140));
            shutdownSignal.await();
        } catch (IOException exc) {
            LOG.error().append(exc).commit();
        }
    }

    private void setTbProcess(Process process) {
        tbProcess = process;
    }

    public static void main(String[] args) {
        new DeathTestRunner(args).start();
    }
}
