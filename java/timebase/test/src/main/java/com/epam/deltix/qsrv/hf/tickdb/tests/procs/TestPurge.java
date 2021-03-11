package com.epam.deltix.qsrv.hf.tickdb.tests.procs;

import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.DBWrapper;
import com.epam.deltix.util.cmdline.DefaultApplication;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestPurge extends DefaultApplication implements Runnable {

    private static final Log LOG = LogFactory.getLog(TestPurge.class);

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private String streamKey;

    public TestPurge(String[] args) {
        super(args);
    }

    @Override
    public void run() {
        long purgePeriod = getLongArgValue("-purgePeriod", 60 * 1000);
        long purgeInterval = getLongArgValue("-purgeInterval", 2 * 60 * 1000);
        String streamKey = getArgValue("-stream", "testStream");
        String dbUrl = getArgValue("-db", "dxtick://localhost:8011");
        try (DBWrapper dbWrapper = new DBWrapper(dbUrl)) {
            try {
                this.streamKey = streamKey;
                scheduledExecutorService.scheduleAtFixedRate(new PurgeTask(dbWrapper, purgeInterval), purgePeriod, purgePeriod, TimeUnit.MILLISECONDS);
                scheduledExecutorService.awaitTermination(10, TimeUnit.HOURS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class PurgeTask implements Runnable {
        private final long purgeInterval;
        private final DBWrapper wrapper;

        PurgeTask(DBWrapper wrapper, long purgeInterval) {
            this.wrapper = wrapper;
            this.purgeInterval = purgeInterval;
        }

        @Override
        public void run() {
            DXTickStream stream = wrapper.getDB().getStream(streamKey);
            long from = System.currentTimeMillis() - purgeInterval;
            LOG.info().append("PURGE to ").appendTimestamp(from).commit();
            stream.purge(from);
        }
    }

    public static void main(String[] args) {
        new TestPurge(args).start();
    }

    public static TestPurge create(String dbUrl, String stream, long purgePeriod, long purgeInterval) {
        return new TestPurge(new String[]{
                "-db", dbUrl,
                "-stream", stream,
                "-purgePeriod", Long.toString(purgePeriod),
                "-purgeInterval", Long.toString(purgeInterval)
        });
    }
}
