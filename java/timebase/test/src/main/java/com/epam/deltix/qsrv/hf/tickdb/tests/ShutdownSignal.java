package com.epam.deltix.qsrv.hf.tickdb.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by Alex Karpovich on 10/05/2021.
 */
public class ShutdownSignal {

    private final CountDownLatch latch = new CountDownLatch(1);

    public ShutdownSignal() {
        final Thread shutdownHook = new Thread(this::signal, "shutdown-signal-hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public boolean isSignaled() {
        return latch.getCount() <= 0;
    }

    /**
     * Programmatically signal awaiting threads.
     */
    public void signal() {
        latch.countDown();
    }

    /**
     * Await the reception of the shutdown signal.
     */
    public void await() {
        try {
            latch.await();
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean await(final long timeout, final TimeUnit unit) {
        try {
            return latch.await(timeout, unit);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
