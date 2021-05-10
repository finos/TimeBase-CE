package com.epam.deltix.qsrv.hf.tickdb.tests.procs;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import javax.annotation.Nonnull;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class TestProcess implements Runnable {

    private final static Log LOG = LogFactory.getLog(TestProcess.class);

    protected final Runnable runnable;

    private TestProcess(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            return thread;
        });
        executor.execute(runnable);

        // gracefully exit, cause standard Process does not support it via methods like destroy, etc.
        new Scanner(System.in).nextLine();
        LOG.info().append("Received exit signal.").commit();
//        try {
//            executor.awaitTermination(10, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            LOG.error().append(e).commit();
//        }
//        System.exit(0);
    }

    public static TestProcess create(Runnable runnable) {
        return new TestProcess(runnable);
    }

}
