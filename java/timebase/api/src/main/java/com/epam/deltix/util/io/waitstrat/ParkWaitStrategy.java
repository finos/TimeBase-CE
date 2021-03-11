package com.epam.deltix.util.io.waitstrat;

import com.epam.deltix.util.lang.Changeable;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 *
 */
public class ParkWaitStrategy implements WaitStrategy {
    @SuppressWarnings("unused")
    private volatile Thread owner;

    private static final AtomicReferenceFieldUpdater<ParkWaitStrategy, Thread> ownerAccess =
        AtomicReferenceFieldUpdater.newUpdater(ParkWaitStrategy.class, Thread.class, "owner");

    public ParkWaitStrategy() {
        owner = null;
    }

    @Override
    public void                         waitSignal() throws InterruptedException {
        Thread t = Thread.currentThread();
        if (!ownerAccess.compareAndSet(this, null, t)) {
            throw new IllegalStateException("A second thread tried to acquire a signal barrier that is already owned.");
        }

        LockSupport.park(this);

        // If a thread has called #signal() the owner should already be null.
        // However the documentation for LockSupport.unpark makes it clear that
        // threads can wake up for absolutely no reason. Do a compare and set
        // to make sure we don't wipe out a new owner, keeping in mind that only
        // thread should be awaiting at any given moment!
        ownerAccess.compareAndSet(this, t, null);

        // Check to see if we've been unparked because of a thread interrupt.
        if (t.isInterrupted())
            throw new InterruptedException();
    }

    @Override
    public void                         signal() {
        Thread t = ownerAccess.getAndSet(this, null);
        if (t != null)
            LockSupport.unpark(t);
    }

    @Override
    public void close() {
        signal();
    }
}