package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.SSLProperties;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.time.Interval;
import com.epam.deltix.util.time.Periodicity;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBStress;

@Category(TickDBStress.class)
public class StressTest_TBServer {

    private static TDBRunner runner;

    private static final String [] SYMBOLS = { "ORCL", "AAPL", "IBM", "GOOG" };

    @BeforeClass
    public static void start() throws Throwable {

        File tb = new File(TDBRunner.getTemporaryLocation());
        QSHome.set(tb.getParent());

        StartConfiguration config = StartConfiguration.create(true, false, false);
        config.tb.setSSLConfig(new SSLProperties(true, true));
        runner = new TDBRunner(true, true, tb.getAbsolutePath(), new TomcatServer(config));
        runner.startup();

        TickDBCreator.createBarsStream(runner.getServerDb(), TickDBCreator.BARS_STREAM_KEY);
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void testConnects() {

        for (int i = 0; i < 1000; i++) {
            try (DXTickDB db = TickDBFactory.createFromUrl("dxtick://localhost:" + runner.getPort())) {
                db.open(false);
                db.close();
            }
        }
    }

    @Test
    public void             testReset () throws InterruptedException {
        readAndReset (100, 0);
        resetAfterClose(100);
    }

    private DXTickStream getBars () {
        DXTickDB tickdb = runner.getTickDb();
        DXTickStream tickStream = tickdb.getStream(TickDBCreator.BARS_STREAM_KEY);
        if (tickStream == null)
            tickStream = TickDBCreator.createBarsStream (tickdb, TickDBCreator.BARS_STREAM_KEY);
        return tickStream;
    }

    @Test
    public void             testSimpleReset () throws InterruptedException {
        readAndSimpleReset (100, 0);
    }

    @Test
    public void             sleepyReader () throws InterruptedException {
        readAndReset (10, 1000);
    }

    public void             readAndReset (int numTimes, int timeout) throws InterruptedException {

        TickCursor cur = TickCursorFactory.create(getBars(), 0, "GOOG", "AAPL");

        try {
            for (int jj = 0; jj < numTimes; jj++) {
                //System.out.println("Run #" + jj);

                for (int ii = 0; ii < 1000; ii++)
                    assertTrue (cur.next ());

                // Wait for server to fill buffer
                if (timeout != 0)
                    Thread.sleep (timeout);

                //  reset same symbol twice in a row
                String          symbol = SYMBOLS [(jj >> 1) & 0x3];

                cur.clearAllEntities();
                cur.setTimeForNewSubscriptions(1104762660000L);
                //cur.reset (1104762660000L, FeedFilter.createEquitiesFilter (symbol));
                cur.addEntity(new ConstantIdentityKey(symbol));
                //System.out.println("Run #" + jj + " reset");

                assertTrue (cur.next ());

                InstrumentMessage msg = cur.getMessage ();
                assertEquals ("Run #" + jj, 1104762660000L, msg.getTimeStampMs());
                assertEquals ("Run #" + jj, msg.getSymbol().toString (), symbol);
            }
        } finally {
            cur.close ();
        }
    }

    public void             resetAfterClose (int timeout) throws InterruptedException {
        TickCursor cur = TickCursorFactory.create(getBars(), 0, "GOOG", "AAPL");

        try {
            while (cur.next());

            // Wait for server to fill buffer
            if (timeout != 0)
                Thread.sleep (timeout);

            String          symbol = "AAPL";
//                    jj == 0 ? "ORCL" :
//                    jj == 1 ? "AAPL" :
//                    jj == 2 ? "IBM" :
//                        "GOOG";

            cur.clearAllEntities();
            cur.setTimeForNewSubscriptions(1104762660000L);
            cur.addEntity(new ConstantIdentityKey(symbol));

            //cur.reset (1104762660000L, FeedFilter.createEquitiesFilter (symbol));
            //System.out.println("Run #" + jj + " reset");

            assertTrue (cur.next ());

            InstrumentMessage   msg = cur.getMessage ();
            assertEquals (1104762660000L, msg.getTimeStampMs());
            assertEquals (msg.getSymbol().toString (), symbol);
        } finally {
            cur.close ();
        }
    }

    public void             readAndSimpleReset (int numTimes, int timeout) throws InterruptedException {

        TickCursor cur = TickCursorFactory.create(getBars (), 1104762660000L,
                "GOOG", "AAPL");

        try {
            for (int jj = 0; jj < numTimes; jj++) {
                //System.out.println("Run #" + jj);

                for (int ii = 0; ii < 1000; ii++) {
                    assertTrue (cur.next ());
                }

                // Wait for server to fill buffer
                if (timeout != 0)
                    Thread.sleep (timeout);

                //  reset same symbol twice in a row
                //String          symbol = SYMBOLS [(jj >> 1) & 0x3];

                cur.reset (1104762660000L);
                //System.out.println("Run #" + jj + " reset");

                assertTrue (cur.next ());
                assertEquals ("Run #" + jj, 1104762660000L, cur.getMessage().getTimeStampMs());
                String symbol = cur.getMessage().getSymbol().toString();

                assertTrue (cur.next ());
                assertEquals ("Run #" + jj, 1104762660000L, cur.getMessage().getTimeStampMs());
                if ("GOOG".equals(symbol))
                    assertEquals ("Run #" + jj, "AAPL", cur.getMessage().getSymbol().toString());
                else
                    assertEquals ("Run #" + jj, "GOOG", cur.getMessage().getSymbol().toString());
            }
        } finally {
            cur.close ();
        }
    }

    @Test
    public void testStreams() {
        TickDBClient client = (TickDBClient) runner.getTickDb();

        String name = "testStream";
        DXTickStream stream = client.createStream (name, name, name, 0);
        for (int i = 0; i < 200; i++) {
            client.listStreams();
            StreamConfigurationHelper.setBar (
                    stream, "", null, Interval.MINUTE,
                    "DECIMAL(4)",
                    "DECIMAL(0)"
            );
            stream.setPeriodicity(Periodicity.mkRegular(Interval.DAY));
        }

    }

    @Test
    public void testCreateStreams() {
        long time = System.currentTimeMillis();

        DXTickDB db = runner.getTickDb();

        StreamOptions options = getBars().getStreamOptions();

        String name = "testme";
        for (int i = 0; i < 1000; i++) {
            options.name = name + i;
            db.createStream (name + i, options);
        }

        long end = System.currentTimeMillis();
        double                          s = (end - time) * 0.001;

        System.out.printf ("creation time: %,.3fs;\n", s);
    }


    @Test
    public void             openCloseCycle() {
        DXTickStream bars = getBars();

        for (int ii = 0; ii < 1000; ii++) {
            TickCursor cur = TickCursorFactory.create(bars, 0, "GOOG", "AAPL");
            //cur.next();
            cur.close ();
        }
    }

    @Test
    public void             openCloseCycle2() throws InterruptedException {

        int TOTAL = 50;
        boolean doInterrupt = false;
        final MutableInt done = new MutableInt();

        final DXTickStream bars = getBars();
        ArrayList<Thread> threads = new ArrayList<Thread>();

        for (int ii = 0; ii < TOTAL; ii++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 100; i++) {
                        try {
                            try (TickCursor cur = TickCursorFactory.create(bars, 0,
                                    "GOOG", "AAPL")) {
                                cur.close();
                            }
                            if (Thread.interrupted())
                                throw new InterruptedException();
                        } catch (InterruptedException e) {

                        } catch (UncheckedInterruptedException e) {

                        }
                    }
                    done.increment();
                }
            };

            Thread thread = new Thread(runnable);
            threads.add(thread);
            thread.start();
        }

        if (doInterrupt) {
            Random rnd = new Random(2010);
            while (done.intValue() != TOTAL) {
                int index = rnd.nextInt(100);
                threads.get(index).interrupt();
                Thread.sleep(10);
            }
        }

        for (int i = 0; i < threads.size(); i++) {
            Thread thread = threads.get(i);
            thread.join();
        }

        Thread.sleep(10000);
    }

    public void             complexOpenCloseCycle () throws InterruptedException {
        final CountDownLatch done = new CountDownLatch(2);
        getBars();

        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {
                DXTickStream bars = getBars();

                for (int ii = 0; ii < 1000; ii++) {
                    TickCursor cur = TickCursorFactory.create(bars, 0, "GOOG", "AAPL");
                    cur.next ();
                    cur.close ();
                }

                for (int ii = 0; ii < 1000; ii++) {
                    TickCursor cur = runner.getTickDb().createCursor(null);
                    cur.reset(0);
                    cur.subscribeToAllEntities();
                    cur.subscribeToAllTypes();
                    cur.addStream(getBars());

                    cur.next ();
                    cur.close ();
                }

                done.countDown();
            }
        };

        Runnable runnable2 = new Runnable() {
            @Override
            public void run() {
                DXTickStream bars = getBars();

                for (int ii = 0; ii < 1000; ii++) {
                    TickCursor cur = TickCursorFactory.create(bars, 0, "GOOG", "AAPL");
                    cur.next ();
                    cur.close ();
                }

                done.countDown();
            }
        };

        new Thread(runnable1).start();
        new Thread(runnable2).start();

        done.await();
    }

    @Test
    public void             complexOpenCloseCycle1 () throws InterruptedException {

        final DXTickDB tickDb = runner.getTickDb();

        final int numThreads = 100;
        final int numCursors = 10;

        final CountDownLatch done = new CountDownLatch(numThreads);

        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {

                for (int ii = 0; ii < numCursors; ii++) {
                    TickCursor cur = tickDb.createCursor(new SelectionOptions(true, false, false));
                    cur.close ();
                }

                done.countDown();

            }
        };

        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++)
            (threads[i] = new Thread(runnable1, "Cursor Test Thread [" + i + "]")).start();

        for (int i = 0; i < numThreads; i++)
            threads[i].join();

        done.await();
    }

    @Test
    public void             complexOpenCloseCycle2 () throws InterruptedException {

        final int numThreads = 100;
        final int numLoaders = 10;

        final CountDownLatch done = new CountDownLatch(numThreads);

        final DXTickStream bars = getBars();

        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {

                try {
                    for (int ii = 0; ii < numLoaders; ii++) {
                        TickLoader loader = bars.createLoader();
                        loader.close ();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    done.countDown();
                }

            }
        };

        Thread[] threads = new Thread[numThreads];

        for (int i = 0; i < numThreads; i++)
            (threads[i] = new Thread(runnable1, "Loader Test Thread [" + i + "]")).start();

        done.await();

        for (int i = 0; i < numThreads; i++)
            threads[i].join();
    }

    @Test
    public void             testWaitWithEndedCursor () throws InterruptedException {
        TickStream              s = getBars ();

        IdentityKey[]   ents = s.listEntities ();
        Random                  r = new Random (2009);
        //FeedFilter              filter = FeedFilter.createUnrestrictedNoSymbols ();
        TickCursor              c = s.select (0, null, null, new IdentityKey[0]);

        try {
            for (int ii = 0; ii < 10; ii++) {
                String              symbol =
                        ents [r.nextInt (ents.length)].getSymbol ().toString ();

                c.clearAllEntities();
                c.setTimeForNewSubscriptions(0);
                c.addEntity(new ConstantIdentityKey(symbol));

                while (c.next ())
                    c.getMessage ();

                //  This causes the server download thread to go into a wait
                //  for reset or disconnect.

                Thread.sleep (500);
            }
        } finally {
            c.close ();
        }
    }

    @Test
    public void testStreamsCache() {
        TickDBClient client = (TickDBClient) runner.getTickDb();

        DXTickDB client1 = new TickDBClient("localhost", runner.getPort());
        client1.open(false);
        int before = client1.listStreams().length;

        String name = "testcache";
        DXTickStream stream = client.createStream (name, name, name, 0);
        for (int i = 0; i < 200; i++) {
            String key = String.valueOf(i);
            client.createStream(key,
                    StreamOptions.fixedType(StreamScope.DURABLE, key, null, 1, StreamConfigurationHelper.mkUniversalBarMessageDescriptor()));

            stream.setPeriodicity(Periodicity.mkRegular(Interval.DAY));
        }

        assertEquals(before + 201, client1.listStreams().length);

        for (int i = 0; i < 100; i++) {
            DXTickStream stream1 = client.getStream(String.valueOf(i * 2));
            stream1.delete();
        }

        assertEquals(before + 101, client1.listStreams().length);

        client1.close();
    }

}
