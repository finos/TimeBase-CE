package com.epam.deltix.test.qsrv.dtb.store.impl;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.dtb.StreamTestHelpers;
import com.epam.deltix.qsrv.dtb.fs.local.FailingPathImpl;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.Home;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDBStress.class)
public class Test_SteamResets {
    private static final Log LOG = LogFactory.getLog(FailingPathImpl.class);

    private static final long DURATION = TimeUnit.MINUTES.toMillis(5);
    private static final boolean printTime = false; // Set to true to print extremely not precise time

    TickDBImpl tickDB;
    DXTickStream testStream;

    @Before
    public void startup() throws Exception {
        String temporaryLocation = getTemporaryLocation();
        LOG.info("Temp DB location: " + temporaryLocation);
        File folder = new File(temporaryLocation);

        tickDB = new TickDBImpl(folder);
        tickDB.open(false);

        this.testStream = StreamTestHelpers.createTestStream(tickDB, "test1");
    }

    @After
    public void shutdown() throws Exception {
        tickDB.close();
        tickDB.format();
    }

    /**
     * TB failed with OOM if we often do cursor.reset(...) because we did not release resources properly (and produced memory leak).
     * This test ensures that TB is able to continuously run with frequent resets and without OOM.
     */
    @Test
    public void oomOnManyResets() throws InterruptedException {
        StreamTestHelpers.MessageGenerator loader1 = StreamTestHelpers.createDefaultLoaderRunnable(testStream);
        Thread loaderThread1 = new Thread(loader1);


        loaderThread1.start();

        Thread timeThread = new Thread(new TimerRunnable());
        if (printTime) {
            timeThread.start();
        }

        SelectionOptions so = new SelectionOptions();

        String[] symbols = StreamTestHelpers.getDefatulSymbols();
        IdentityKey[] instruments = new IdentityKey[symbols.length];
        for (int i = 0; i < symbols.length; i++) {
            instruments[i] = new ConstantIdentityKey(symbols[i]);
        }

        TickCursor cursor = tickDB.createCursor(so, testStream);
        String[] tickMessageTypeStrings = {TradeMessage.class.getName ()};

        long startTime = System.currentTimeMillis();
        long endTime = startTime + DURATION;

        while (System.currentTimeMillis() < endTime) {
            testResets(cursor, testStream, instruments, tickMessageTypeStrings);
        }
        cursor.close();

        loaderThread1.interrupt();
        if (printTime) {
            timeThread.interrupt();
        }
    }

    private void testResets(TickCursor cursor, DXTickStream stream, IdentityKey[] instruments, String[] tickMessageTypeStrings) {
        cursor.clearAllEntities();
        cursor.addEntities(instruments, 0, instruments.length);
        cursor.removeAllStreams();
        cursor.addStream(stream);
        cursor.addTypes(tickMessageTypeStrings);
        cursor.reset(System.currentTimeMillis());
    }

    private static String getTemporaryLocation() {
        return getTemporaryLocation("tickdb");
    }

    private static String getTemporaryLocation(String subpath) {
        File random = Home.getFile("build" + File.separator + "test_temp_db" + File.separator + new GUID().toString() + File.separator + subpath);
        if (random.mkdirs())
            random.deleteOnExit();

        return random.getAbsolutePath();
    }

    private static class TimerRunnable implements Runnable {
        @Override
        public void run() {
            //long startTime = System.currentTimeMillis();
            int count = 0;

            try {
                while (true) {
                    Thread.sleep(1_000);
                    count++;
                    System.out.println("Time: " + count);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}