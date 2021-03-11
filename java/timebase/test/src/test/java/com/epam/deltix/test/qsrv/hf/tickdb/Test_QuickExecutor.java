package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *
 */
@Category(TickDBFast.class)
public class Test_QuickExecutor extends TDBRunnerBase {

    static {
        System.setProperty("QuickExecutor.threads", "50");
    }

    @Test
    public void             complexOpenCloseCycle1 () throws InterruptedException {

        final DXTickDB tickDb = runner.getTickDb();

        final int numThreads = 100;
        final int numCursors = 100;

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

        for (int i = 0; i < numThreads; i++) {
            new Thread(runnable1).start();
        }

        done.await();
    }
}