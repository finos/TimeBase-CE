package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * User: alex
 * Date: Oct 8, 2010
 */
public class Test_ReaderPerformance {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        System.setProperty("agrona.disable.bounds.checks", "true");

        //runner = new TDBRunner(true, false, "D:\\Workshop\\Projects\\QuantServer\\QuantServers\\main\\perf\\tickdb");
        runner = new ServerRunner(true, true);
        runner.options = new DataCacheOptions(Integer.MAX_VALUE, DataCacheOptions.DEFAULT_CACHE_SIZE * 10); // 10G
        runner.startup();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    public void test(DXTickDB db) {

        StreamOptions options1 = new StreamOptions();
        options1.scope = StreamScope.RUNTIME;
        options1.setFlag(TDBProtocol.AF_STUB_STREAM, true);

        DXTickStream stream = db.getStream("stubDataStream");
        if (stream == null)
            stream = db.createStream("stubDataStream", options1);

//        DXTickStream stream = db.getStream("bars1min");
        
        SelectionOptions options = new SelectionOptions(true, false);
        options.channelPerformance = ChannelPerformance.HIGH_THROUGHPUT;
//        IdentityKey[] keys =
//        {
//                new ConstantIdentityKey("MSFT")
//        };

        TickCursor cursor = stream.select(0, options);

        long                            t0 = System.currentTimeMillis ();
        long                            tPrev = t0;
        long                            count = 0;
        long reportInterval = 10_000_000;
        while (cursor.next()) {
            count++;
            cursor.getMessage();
            if (count % reportInterval == 0) {
                long now = System.currentTimeMillis();
                long timeDelta = now - tPrev;
                tPrev = now;
                System.out.printf("Read %,3d messages; speed: %,3d msg/s\n", count, reportInterval * 1000 / timeDelta);
            }
        }

        long                            t1 = System.currentTimeMillis ();
        double                          s = (t1 - t0) * 0.001;
        System.out.printf (
            "%,d messages in %,.3fs; speed: %,.0f msg/s\n",
            count,
            s,
            count / s
        );
        
        cursor.close();

//        long                            t2 = System.currentTimeMillis ();
//
//        s = (t2 - t0) * 0.001;
//        System.out.printf (
//            "Overall: %,d messages in %,.3fs; speed: %,.0f msg/s\n",
//            count,
//            s,
//            count / s
//        );
    }

    public static void main (String [] args) throws Throwable {
        Test_ReaderPerformance.start();

        Test_ReaderPerformance tester = new Test_ReaderPerformance();
//        try (DXTickDB db = TickDBFactory.createFromUrl("dxtick://localhost:8011")) {
//            db.open(false);
//            tester.test(db);
//            tester.test(db);
//        }

        tester.test(runner.getTickDb());
        tester.test(runner.getTickDb());

        Test_ReaderPerformance.stop();
    }
}