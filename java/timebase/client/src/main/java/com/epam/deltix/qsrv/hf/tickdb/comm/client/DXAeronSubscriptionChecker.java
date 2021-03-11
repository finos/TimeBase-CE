package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import io.aeron.Subscription;
import io.aeron.logbuffer.ControlledFragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

import javax.annotation.concurrent.GuardedBy;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class lets to support async-style cursor API (availability listeners) with Aeron.
 * <p>
 * Problem: Aeron does not provide "new data callbacks". To check if data available we must actively poll subscription.
 * So to support our async cursor API we need to have additional thread(s) to do that.
 * To reduce thread overhead we use single thread to check all subscriptions for all cursors in single client (TickDBClient) instance.
 * <p>
 * Some cursors are low latency cursors. If we have at least one low latency cursor that wants to use aync API we should
 * not let thread sleep or wait. To track such cursory they mist be registered via {@link #registerCritical(Object)}.
 *
 * @author Alexei Osipov
 */
public class DXAeronSubscriptionChecker {
    private final Object lock = new Object();

    @GuardedBy("lock")
    private Entry[] entries;
    @GuardedBy("lock")
    private int count = 0;

    @GuardedBy("lock")
    private boolean threadStarted = false;
    @GuardedBy("lock")
    private Thread thread;

    private volatile boolean stopped = false;

    private final ConcurrentHashMap<Object, Boolean> criticalSubscribers = new ConcurrentHashMap<>();

    private final IdleStrategy idleStrategy = new BackoffIdleStrategy(1, 1, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));
    private final IdleStrategy latencyCriticalStrategy = new YieldingIdleStrategy();

    public DXAeronSubscriptionChecker() {
        this.entries = new Entry[16];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = new Entry();
        }
    }

    /**
     * Adds subscription to be polled by "checker" thread.
     * Checker thread will call the listener one time as soon as there is new data.
     * Then "checker" will stop polling this subscription.
     *
     * @param subscription subscription to check
     * @param listener listener to call when there is new data
     */
    public void addSubscriptionToCheck(Subscription subscription, Runnable listener) {
        synchronized (lock) {
            for (int i = 0; i < count; i++) {
                Entry entry = entries[i];
                if (entry.subscription == subscription) {
                    // Do not add subscription if it already exists
                    return;
                }
            }

            int newCount = count + 1;
            ensureCapacity(newCount);

            Entry entry = entries[count];
            entry.subscription = subscription;
            entry.listener = listener;
            count = newCount;

            if (!startThreadIfNecessary()) {
                lock.notifyAll();
            }
        }
    }

    /**
     * This method can be used to register latency critical cursors
     * @param criticalSubscriber any object that represents latency critical context
     */
    public void registerCritical(Object criticalSubscriber) {
        if (stopped) {
            throw new IllegalStateException("Checker is stopped");
        }
        Boolean result = criticalSubscribers.putIfAbsent(criticalSubscriber, Boolean.TRUE);
        assert result == null;

        // Prepare thread to make it immediately available for critical subscriptions
        synchronized (lock) {
            if (!startThreadIfNecessary()) {
                lock.notifyAll();
            }
        }
    }

    public void unregisterCritical(Object criticalSubscriber) {
        Boolean result = criticalSubscribers.remove(criticalSubscriber);
        assert result != null;
    }

    private boolean startThreadIfNecessary() {
        if (threadStarted) {
            return false;
        }
        this.thread = new Thread(new SubscriptionChecker());
        //thread.setDaemon(true); // TODO: Check if we can allow thread to be daemon
        thread.setName("AeronClientSubscriptionChecker");
        thread.start();
        threadStarted = true;
        return true;
    }

    private void ensureCapacity(int newCount) {
        int capacity = entries.length;
        if (newCount > capacity) {
            int newCapacity = capacity * 2;
            Entry[] newArray = new Entry[newCapacity];
            System.arraycopy(entries, 0, newArray, 0, capacity);
            for (int i = capacity; i < newCapacity; i++) {
                newArray[i] = new Entry();
            }
            entries = newArray;
        }
    }

    public void stop() {
        stopped = true;
        Thread thread;
        synchronized (lock) {
            lock.notifyAll();
            thread = this.thread;
        }
        try {
            if (thread != null) {
                thread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        if (!criticalSubscribers.isEmpty()) {
            throw new IllegalStateException("Some subscribers not unregistered themselves");
        }
    }


    private static class Entry {
        Subscription subscription = null;
        Runnable listener = null;
    }

    private class SubscriptionChecker implements Runnable {

        private final FragmentHandlerForNewDataCheck dataCheckHelper = new FragmentHandlerForNewDataCheck();

        @Override
        public void run() {
            Runnable listenerToRun = null;
            while (!stopped) {
                // Run pending listener (outside of lock)
                // Note: this implementation will run only one listener per loop executions.
                // If there a case when ma
                if (listenerToRun != null) {
                    listenerToRun.run();
                    listenerToRun = null;
                }

                synchronized (lock) {
                    boolean entriesRemoved = false;

                    int pos = 0;
                    while (pos < count) {
                        Entry entry = entries[pos];
                        if (hasData(entry.subscription)) {
                            Runnable listener = entry.listener;

                            if (pos + 1 < count) {
                                // Move the last entry data to current position
                                Entry lastEntry = entries[count - 1];
                                entry.listener = lastEntry.listener;
                                entry.subscription = lastEntry.subscription;
                                lastEntry.subscription = null;
                                lastEntry.listener = null;
                            } else {
                                // This is last entry
                                entry.subscription = null;
                                entry.listener = null;
                            }
                            count --;
                            entriesRemoved = true;

                            listenerToRun = listener;
                            // We have to stop and exit the inner while loop to be able to call listener without holding the lock
                            break;
                        } else {
                            pos ++;
                        }
                    }
                    if (entriesRemoved) {
                        idleStrategy.reset();
                        latencyCriticalStrategy.reset();
                    } else {
                        boolean hasLowLatencySubscribers = criticalSubscribers.isEmpty();
                        if (count > 0 || hasLowLatencySubscribers) {
                            if (hasLowLatencySubscribers) {
                                idleStrategy.idle();
                            } else {
                                latencyCriticalStrategy.idle();
                            }
                        } else {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                stopped = true;
                            }
                        }
                    }
                }
            }
        }

        private boolean hasData(Subscription subscription) {
            dataCheckHelper.reset();
            int count = subscription.controlledPoll(dataCheckHelper, 1);
            assert count == 0; // We should never actually read data from subscription
            return dataCheckHelper.gotData();
        }
    }

    private static class FragmentHandlerForNewDataCheck implements ControlledFragmentHandler {
        private boolean gotData;

        public void reset() {
            gotData = false;
        }

        public boolean gotData() {
            return gotData;
        }

        @Override
        public Action onFragment(DirectBuffer buffer, int offset, int length, Header header) {
            gotData = true;
            return Action.ABORT;
        }
    }
}
