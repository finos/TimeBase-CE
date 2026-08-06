package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.hf.spi.conn.ReconnectableImpl.ReconnectIntervalAdjuster;
import com.epam.deltix.util.time.GlobalTimer;
import com.epam.deltix.util.time.TimerRunner;
import net.jcip.annotations.GuardedBy;

import java.util.Objects;
import java.util.TimerTask;

/**
 *  Similar to {@link com.epam.deltix.qsrv.hf.spi.conn.ReconnectableImpl}
 *  but {@link #connected()} and {@link #disconnected()} do not execute callbacks directly.
 */
class TickDBReconnectableImpl {
    protected static final Log LOG = LogFactory.getLog("tickdb.client");

    private volatile long initialReconnectInterval = 5000;
    private volatile ReconnectIntervalAdjuster adjuster = null;
    private volatile String logprefix;
    private static final LogLevel logLevel = LogLevel.DEBUG;

    private final Object lockObject;

    @GuardedBy("lockObject")
    private TickDBClient reconnector = null;

    private volatile boolean isConnected = false;

    @GuardedBy("lockObject")
    private long timeDisconnected;

    @GuardedBy("lockObject")
    private int numReconnectAttempts;

    @GuardedBy("lockObject")
    private long currentReconnectInterval;

    @GuardedBy("lockObject")
    private TimerTask reconnectTask;

    @GuardedBy("lockObject")
    private String lastExceptionAsString;

    /**
     * @param lockObject an object to use as lock for synchronization instead of "this"
     */
    public TickDBReconnectableImpl(String logprefix, Object lockObject) {
        this.logprefix = logprefix;
        // Use this as lock object if not provided. That's preserves old behavior.
        this.lockObject = Objects.requireNonNull(lockObject, "lockObject may not be null");
    }

    public ReconnectIntervalAdjuster getAdjuster() {
        return adjuster;
    }

    public void setAdjuster(ReconnectIntervalAdjuster adjuster) {
        this.adjuster = adjuster;
    }

    public void setReconnector(TickDBClient reconnector) {
        this.reconnector = reconnector;
    }

    public long getInitialReconnectInterval() {
        return initialReconnectInterval;
    }

    public void setInitialReconnectInterval(long initialReconnectInterval) {
        this.initialReconnectInterval = initialReconnectInterval;
    }

    public void setLogPrefix(String logprefix) {
        this.logprefix = logprefix;
    }

    public void connected() {
        synchronized (lockObject) {
            if (reconnectTask != null)
                reconnectTask.cancel();

            isConnected = true;
            lastExceptionAsString = null;
        }

        LOG.log(logLevel, "[%s] Connected").with(logprefix);

        // onReconnected(); // deferred to fireOnReconnected()
    }

    public void disconnected() {
        synchronized (lockObject) {
            isConnected = false;
            timeDisconnected = System.currentTimeMillis();
        }

        LOG.log(logLevel, "[%s] Disconnected").with(logprefix);

        // onDisconnected(); // deferred to fireOnDisconnected()
    }

    public boolean isConnected() {
        synchronized (lockObject) {
            return isConnected;
        }
    }

    private void tryReconnect() {
        synchronized (lockObject) {
            reconnectTask = null;

            boolean reschedule = false;

            if (!isConnected && reconnector != null) {
                try {
                    reschedule =
                            reconnector.tryReconnect(
                                    numReconnectAttempts,
                                    System.currentTimeMillis() - timeDisconnected,
                                    this
                            );
                } catch (Throwable x) {
                    String check = x.toString();
                    if (check.equals(lastExceptionAsString)) {
                        LOG.log(logLevel, "[%s] Reconnect failed due to: %s").with(logprefix).with(lastExceptionAsString);
                    } else {
                        LOG.log(logLevel, "[%s] Reconnect failed: %s").with(logprefix).with(x);
                        lastExceptionAsString = check;
                    }

                    reschedule = true;
                }

                numReconnectAttempts++;
            }

            if (!isConnected && reschedule) {
                ReconnectIntervalAdjuster adj = adjuster;

                if (adj != null)
                    currentReconnectInterval =
                            adj.nextInterval(
                                    numReconnectAttempts,
                                    timeDisconnected,
                                    currentReconnectInterval
                            );

                scheduleTask();
            }
        }
    }

    @GuardedBy("lockObject")
    private void scheduleTask() {
        assert Thread.holdsLock(lockObject);

        reconnectTask =
                new TimerRunner() {
                    @Override
                    public void runInternal() {
                        try {
                            tryReconnect();
                        } catch (Throwable x) {
                            LOG.error("[%s] Unexpected: %s").with(logprefix).with(x);
                        }
                    }
                };

        GlobalTimer.INSTANCE.schedule(reconnectTask, currentReconnectInterval);

        LOG.log(logLevel, "[%s] Next reconnect in %s").with(logprefix).with(currentReconnectInterval);
    }

    public void scheduleReconnect() {
        synchronized (lockObject) {
            if (reconnector == null)
                throw new IllegalStateException("[" + logprefix + "] Call setReconnector() first.");

            if (reconnectTask != null)
                reconnectTask.cancel();

            numReconnectAttempts = 0;
            currentReconnectInterval = initialReconnectInterval;
            scheduleTask();
        }
    }

    public void cancelReconnect() {
        synchronized (lockObject) {
            if (reconnectTask != null) {
                reconnectTask.cancel();
            }
        }
    }

    interface Reconnector {
        /**
         *  Try and reconnect. If successful, this method must call
         *  {@link TickDBReconnectableImpl#connected} on <code>helper</code>. After that, the return
         *  value is irrelevant. If unsucessful, this method can either throw
         *  an exception, or return <code>true</code> to reschedule the reconnect,
         *  or, in rare instances, return <code>false</code> to give up.
         *
         * @return  Whether reconnection should be rescheduled.
         * @throws Exception
         *          If thrown, reconnect will be rescheduled. Therefore, this
         *          method can freely throw exceptions due to reconnect failure.
         *
         */
        boolean tryReconnect(
                int numAttempts,
                long timeSinceDisconnected,
                TickDBReconnectableImpl helper
        ) throws Exception;
    }
}
