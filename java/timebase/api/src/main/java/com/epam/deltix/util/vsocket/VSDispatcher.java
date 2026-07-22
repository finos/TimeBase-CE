/*
 * Copyright 2024 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.util.vsocket;

import com.epam.deltix.thread.affinity.AffinityThreadFactoryBuilder;
import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.collections.generated.ObjectHashSet;
import static com.epam.deltix.util.vsocket.VSProtocol.*;

import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.lang.*;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.time.TimerRunner;
import com.epam.deltix.util.memory.DataExchangeUtils;
import net.jcip.annotations.GuardedBy;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 *
 */
public final class VSDispatcher implements Disposable {

    private final ContextContainer contextContainer;
    private final ThreadFactory                     transportChannelThreadFactory;


    private final Date                              creationDate = new Date ();
    private Timer                                   timer;

    @GuardedBy("transportChannels")
    private final ObjectHashSet<VSTransportChannel> transportChannels =
            new ObjectHashSet<> ();

    /*
        Limit for the number of open channels to prevent potential DDoS attacks or user errors.
        By default, there is no limit (-1 value)
     */

    private int                                     channelsLimit = -1;

    /**
     * Set to non-null value during transport channel recovery.
     * If multiple transport channels are being recovered, this future will be completed with value "true" if all of
     * them are recovered successfully, or "false" if at least one of them failed to recover.
     */

    @GuardedBy("freeChannels")
    // TODO: Replace by Deque
    private final Stack <VSTransportChannel>        freeChannels =
            new Stack <> ();

    private final ArrayList <VSChannelImpl>         channels =
            new ArrayList <> (10);
    //private volatile boolean                        hasAvailableTransport = false;

    volatile VSConnectionListener                   connectionListener = null;
    volatile ConnectionStateListener                stateListener;

    private int                                     activeChannels;
    private int                                     lingerInterval; // How long Dispatcher will wait for reconnection to happen

    private String                                  address;
    private final String                            clientAddress;
    private String                                  applicationID;

    // State of Dispatcher on remote side
    private volatile boolean                        remoteConnected = true;
    private volatile long                           throughput = 0;
    private volatile long                           totalBytes = 0; // number of bytes sent
    private final EMA                               average = new EMA(1000 * 60); // 1 minute

    /**
     * State transitions:
     * <ul>
     * <li> INITIAL -> CONNECTED : when first transport channel is added
     * <li> CONNECTED -> CONNECTING : when at least one transport channel is lost
     * <li> CONNECTING -> CONNECTED : when ALL transport channels are recovered
     * <li> CONNECTING -> DISCONNECTING : when recovery of at least one transport channel fails (explicitly or because of timeout)
     * <li> DISCONNECTING -> DISCONNECTED : when all transport channels are closed after failed recovery
     * </ul>
     */
    private final AtomicReference<VSDispatcherState> state = new AtomicReference<>(VSDispatcherState.INITIAL);

    // Latch that gets counted down when dispatcher is fully closed
    private final CountDownLatch closeLatch = new CountDownLatch(1);

    // Works both as a counter of recovering transport channels
    // and as a barrier to wait until all recovering transports finish recovering.
    private final Phaser recoveringTransports = new Phaser() {
        @Override
        protected boolean onAdvance(int phase, int registeredParties) {
            return false; // never terminate automatically
        }
    };


    private TimerTask flusher = new TimerRunner() {
        private VSChannelImpl[]         list = new VSChannelImpl[10];
        private VSTransportChannel[]    transports = new VSTransportChannel[5];
        private long                    runs = 0;

        @Override
        protected void runInternal() {

            int size;
            synchronized (channels) {
                if ((size = channels.size()) > 0)
                    list = channels.toArray(list);
            }

            for (int i = 0; i < size; i++) {
                VSChannelImpl channel = list[i];
                try {
                    if (channel != null && channel.isAutoflush()) {
                        // For low latency channels (noDelay==true) we do not want to flush all
                        // the accumulated data at once because the remaining data will be sent
                        // by ChannelExecutor shortly. This allows to get more steady rate.
                        // For regular channels (noDelay==false) we want to send all the data
                        // (there is nobody else to do that).
                        channel.getOutputStream().flushAvailable(!channel.getNoDelay());
                    }
                } catch (ChannelClosedException e) {
                    // ignore
                } catch (ConnectionAbortedException e) {
                    VSProtocol.LOGGER.log (Level.WARNING, "Client unexpectedly drop connection. Remote address: " + channel.getRemoteAddress());
                } catch (Exception e) {
                    VSProtocol.LOGGER.log (Level.WARNING, "Exception while flushing data. Remote address: " + channel.getRemoteAddress(), e);
                }
            }

            try {
                // do keep-alive assuming that this task runs every millisecond
                if (runs++ % VSProtocol.KEEP_ALIVE_INTERVAL == 0) {
                    synchronized (transportChannels) {
                        transports = transportChannels.toArray(transports);
                        size = transportChannels.size();
                    }

                    long bytes = 0;
                    for (int i = 0; i < size; i++) {
                        VSTransportChannel transport = transports[i];
                        transport.keepAlive();
                        bytes += transport.socket.getOutputStream().getBytesWritten();
                        bytes += transport.socket.getInputStream().getBytesRead();
                    }

                    throughput = (bytes - totalBytes) / VSProtocol.KEEP_ALIVE_INTERVAL * 1000;
                    average.register(throughput);
                    totalBytes = bytes;
                }
            } catch (Exception ex) {
                VSProtocol.LOGGER.log (Level.WARNING, "Exception while sending transport keep-alive.", ex);
            }
        }
    };

    private final String        clientId;
    private final boolean       isClient;
    private volatile int        index = 0;

    private final HashSet<DisposableListener<VSDispatcher>> listeners = new HashSet<> ();

    /**
     *  Constructs a dispatcher instance for the specified client.
     */
    public VSDispatcher(String clientId, boolean isClient, ContextContainer contextContainer) {
        this.clientId = clientId;

        String[] parts = clientId.split(":");
        this.clientAddress = parts.length > 1 ? parts[0] : null;

        this.isClient = isClient;
        this.contextContainer = contextContainer;

        timer = new Timer ("Flush Timer (" + this + ")", true);
        timer.scheduleAtFixedRate (flusher, 1L, 1L);

        this.transportChannelThreadFactory = new AffinityThreadFactoryBuilder(contextContainer.getAffinityConfig())
                .setNameFormat("VSDispatcher(" + clientId + ") Transport %d")
                .setPriority(Thread.MAX_PRIORITY)
                .setDaemon(true)
                .build();
    }

    /*
        Gets limit for the active channels. Default is -1, meaning no limits.
     */

    public int          getChannelsLimit() {
        return channelsLimit;
    }


     /*
        Sets limit for the active channels. -1 means no limits.
     */

    public void         setChannelsLimit(int limit) {
        if (limit == 0 || limit < -1)
            throw new IllegalArgumentException("Wrong channels limit: " + limit);

        this.channelsLimit = limit;
    }

    /**
     * Return current peak throughput (bytes per second)
     * @return number of bytes per second
     */
    public long                 getThroughput() {
        return throughput;
    }

    /**
     * Return average throughput (bytes per second)
     * @return number of bytes per second
     */
    public double               getAverageThroughput() {
        return average.getAverage();
    }

    public int                  getReconnectInterval() {
        return lingerInterval;
    }

    public String               getApplicationID() {
        if (applicationID == null) {
            applicationID = getApplicationID(clientId);
        }
        return applicationID;
    }

    public static String getApplicationID(String clientId) {
        String[] parts = clientId.split(":");
        return parts.length == 4 ? parts[2] : "<none>";
    }

    public void                 setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public void                 setLingerInterval(int reconnectInterval) {
        this.lingerInterval = reconnectInterval;
    }

    public String               getClientId () {
        return clientId;
    }

    public Date                 getCreationDate () {
        return (creationDate);
    }

    public int                  getNumTransportChannels () {
        synchronized (transportChannels) {
            return (transportChannels.size ());
        }
    }

    public boolean              hasTransportChannels() {
        synchronized (transportChannels) {
            return !transportChannels.isEmpty();
        }
    }

    public boolean isConnectedOrReconnecting() {
        VSDispatcherState value = state.get();
        return value == VSDispatcherState.CONNECTED || value == VSDispatcherState.RECONNECTING;
    }

    public boolean isConnectedAndNotReconnecting() {
        VSDispatcherState value = state.get();
        return value == VSDispatcherState.CONNECTED;
    }

    public void                 addTransportChannel (VSocket socket)
            throws IOException
    {
        VSDispatcherState currentState = state.get();
        if (currentState == VSDispatcherState.DISCONNECTING || currentState == VSDispatcherState.DISCONNECTED) {
            VSProtocol.LOGGER.log (Level.WARNING, "Attempt to add transport channel while dispatcher is disconnecting. Remote address: " + socket.getRemoteAddress() + ". Dispatcher: " + this);
        }

        VSTransportChannel          tc = new VSTransportChannel(this, socket, transportChannelThreadFactory);
        tc.checkedOut = true; // Initially this channel is not in "freeChannels" so it is effectively "checked out"

        // Is that fist transport channel?
        boolean fistConnected = state.compareAndSet(VSDispatcherState.INITIAL, VSDispatcherState.CONNECTED);

        // start transport
        tc.start ();

        synchronized (transportChannels) {

            if (address == null)
                address = socket.getRemoteAddress();

            transportChannels.add (tc);
            transportChannels.notify();
        }

        checkIn(tc);

        // This may be triggered only once per dispatcher lifetime
        if (fistConnected && stateListener != null) {
            stateListener.onConnected();
        }
    }

    public void                 setConnectionListener (VSConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    void                        setStateListener(ConnectionStateListener stateListener) {
        this.stateListener = stateListener;
    }

    @VisibleForTesting
    VSDispatcherState getInternalState() {
        return state.get();
    }

    /**
     * Executed in the context of transport channel thread (VSTransportChannel.run() method) when error occurs on transport.
     *
     * <p>Corresponding transport channel will be closed after this method returns.
     *
     * <p>This method is expected to block until logical transport gets recovered or declared unrecoverably broken.
     * In case of recovery failure, expected to trigger dispatcher shutdown, as loss of single transport channel
     * means loss of data and inconsistent state for client and server.
     *
     * <p>Multiple transport channels may be lost concurrently, so this method may be executed concurrently.
     * In that case, threads may compete for changing dispatcher state.
     */
    void                        transportStopped (VSTransportChannel channel, Throwable ex) {
        if (!(ex instanceof Exception)) {
            // This means major failure, possibly OOM or other serious error.
            VSProtocol.LOGGER.log(Level.SEVERE, "Critical error on transport channel. Remote address: " + channel.socket.getRemoteAddress() + ". Dispatcher: " + this, ex);
            // Just close dispatcher right away
            close();
            return;
        }

        Level disconnectLogLevel = ex instanceof EOFException ? Level.FINE : Level.INFO;
        if (VSProtocol.LOGGER.isLoggable(disconnectLogLevel)) {
            VSProtocol.LOGGER.log(disconnectLogLevel, "Transport channel has stopped. Remote address: " + channel.socket.getRemoteAddress() + ". Error: " + ex.getClass().getSimpleName() + ". Message: " + ex.getMessage());
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + lingerInterval;

        boolean registered = false; // true if we have registered this transport in "recoveringTransports" phaser
        boolean wasCheckedIn;

        try {
            synchronized (transportChannels) {
                VSDispatcherState currentState = state.get();
                if (currentState == VSDispatcherState.DISCONNECTING || currentState == VSDispatcherState.DISCONNECTED) {
                    // Dispatcher is already closing or closed, no need to recover transport
                    return;
                }

                if (!transportChannels.remove(channel)) // check if that channel is already removed
                    return;

                // From this point we consider that we are recovering this transport channel.

                // Counter incremented before state change,
                // so that should be impossible to see CONNECTING with 0 recovering transports and still pending recovery attempt.
                recoveringTransports.register();
                registered = true;
                state.compareAndSet(VSDispatcherState.CONNECTED, VSDispatcherState.RECONNECTING);

                synchronized (freeChannels) {
                    wasCheckedIn = freeChannels.remove(channel);
                    assert wasCheckedIn == !channel.checkedOut;
                    freeChannels.notifyAll();
                }

                if (!wasCheckedIn) {
                    // Try to wait for the channel to become checked in
                    wasCheckedIn = waitForTransportCheckIn(channel, startTime, endTime);
                    if (!wasCheckedIn) {
                        if (VSProtocol.LOGGER.isLoggable(Level.INFO)) {
                            VSProtocol.LOGGER.log(Level.INFO, "Error waiting to reconnect (transport was not checked in).");
                        }
                    }
                }
            }
            ConnectionStateListener stateListener = this.stateListener;

            boolean transportIsUnrecoverablyBroken = false;
            try {
                // trying to recover transport
                VSocketRecoveryInfo recoveryInfo = new VSocketRecoveryInfo(channel.socket, endTime);

                long now = System.currentTimeMillis();
                if (wasCheckedIn && (now < endTime) && !isShutdownState()) {

                    if (stateListener != null) {
                        if (stateListener.onTransportRecoveryStart(recoveryInfo)) {
                            transportIsUnrecoverablyBroken = true;
                        }
                    }

                    if (remoteConnected && !transportIsUnrecoverablyBroken) {
                        // System.out.println("WAITED: remoteConnected=" + remoteConnected + " transportIsUnrecoverablyBroken=" + transportIsUnrecoverablyBroken);
                        try {
                            // We loop here waiting for recovery to complete or timeout to expire or dispatcher to be closed.
                            synchronized (recoveryInfo) {
                                long timeToWait;
                                while ((timeToWait = endTime - now) > 0 && recoveryInfo.isWaitingForRecovery() && remoteConnected && !isShutdownState()) {
                                    recoveryInfo.wait(timeToWait);
                                    if (recoveryInfo.isWaitingForRecovery() && remoteConnected) {
                                        now = System.currentTimeMillis();
                                    }
                                }
                                if (recoveryInfo.isRecoveryFailed()) {
                                    transportIsUnrecoverablyBroken = true;
                                }
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            if (VSProtocol.LOGGER.isLoggable(Level.FINE))
                                VSProtocol.LOGGER.log(Level.FINE, "Error waiting to reconnect.", e);
                        }
                    } else {
                        //System.out.println("NOT WAITED: remoteConnected=" + remoteConnected + " transportIsUnrecoverablyBroken=" + transportIsUnrecoverablyBroken);
                    }
                } else {
                    transportIsUnrecoverablyBroken = true;
                    if (VSProtocol.LOGGER.isLoggable(Level.FINE)) {
                        VSProtocol.LOGGER.log(Level.FINE, "Cancelled recovery of failed connection because of timeout on waiting for check-in from other thread. Remote address: " + getRemoteAddress());
                    }
                }

                // In general, VSDispatcher don't have to shut down if transport recovery fails,
                // because other transport channels may remain functional.
                // However, in our current design, loss of single transport channel means loss of data
                // and inconsistent state for client and server, so we have to shut down the dispatcher.
                // The decision to shut down the dispatcher is delegated to the state listener.
                if (stateListener != null) {
                    if (stateListener.onTransportRecoveryStop(recoveryInfo)) {
                        transportIsUnrecoverablyBroken = true;
                    }
                }
            } finally {
                if (transportIsUnrecoverablyBroken || isShutdownState()) {
                    // We lost this transport channel and were unable to recover it (because of explicit error, timeout or triggered shutdown state).
                    // This means it is not possible to recover from this state, and we have to properly close the dispatcher.
                    // We need to close all remaining connections and explicitly notify user about that.

                    // Record a copy of state listener reference before updating state because it may be changed concurrently.
                    // If save "stateListener" before state update, then we can be sure that
                    // if we had non-null listener before state update, then we will have non-null listener for thread that gets "triggerDisconnectedEvent".
                    var savedStateListener = this.stateListener;

                    // Try to set state to DISCONNECTING, before decrementing recoveringTransports counter,
                    // so other thread will not switch into CONNECTED state if this was the last recovering transport.

                    VSDispatcherState stateBeforeUpdate = state.getAndUpdate(prevState -> {
                        switch (prevState) {
                            case CONNECTED:
                                // Should not happen
                                return VSDispatcherState.DISCONNECTING;
                            case RECONNECTING:
                                return VSDispatcherState.DISCONNECTING;
                            case DISCONNECTING:
                                return prevState; // remain in DISCONNECTING
                            case DISCONNECTED:
                                return prevState; // remain in DISCONNECTED
                            default:
                                throw new IllegalStateException("Unexpected dispatcher state: " + prevState);
                        }
                    });
                    // Disconnected event should be triggered only if we changed the state.
                    // So that event should be triggered only once per dispatcher lifetime.
                    // Also, it disables trigger of onDisconnected event if dispatcher is closed normally via direct call to close().
                    boolean triggerDisconnectedEvent = stateBeforeUpdate == VSDispatcherState.CONNECTED || stateBeforeUpdate == VSDispatcherState.RECONNECTING;

                    registered = false;
                    recoveringTransports.arriveAndDeregister();

                    processChannelRecoveryFailure(ex, triggerDisconnectedEvent, savedStateListener);
                } else {
                    // Successfully recovered this transport channel
                    registered = false;
                    recoveringTransports.arriveAndDeregister();
                    int remaining = recoveringTransports.getUnarrivedParties();
                    if (!recoveringTransports.isTerminated() && remaining == 0) {
                        // All lost transport channels are recovered, try to update state to CONNECTED
                        state.getAndUpdate(prev -> {
                            if (prev == VSDispatcherState.RECONNECTING) {
                                return VSDispatcherState.CONNECTED;
                            } else {
                                return prev; // Keep state unchanged
                            }
                        });
                    }
                }
            }

        } finally {
            if (registered) {
                // In case of any unexpected error, ensure that we release the counter
                recoveringTransports.arriveAndDeregister();
            }
        }
    }

    /**
     * Triggered when transport channel recovery has failed and dispatcher must be disconnected.
     * @param triggerClose if true, then current thread is the one that first detected unrecoverable transport failure and responsible for shutdown
     */
    private void processChannelRecoveryFailure(Throwable ex, boolean triggerClose, @Nullable ConnectionStateListener stateListenerCopy) {
        boolean wasConnected = remoteConnected;

        // notify all waiting for transport that connection is lost
        onRemoteClosed();

        if (ex instanceof SocketException || ex instanceof EOFException || ex instanceof SocketTimeoutException) {
            if (VSProtocol.LOGGER.isLoggable(Level.FINE))
                VSProtocol.LOGGER.log(Level.FINE, "Exception on transport channel. Remote address: " + getRemoteAddress(), ex);
        } else {
            VSProtocol.LOGGER.log(Level.SEVERE, "Exception on transport channel. Remote address: " + getRemoteAddress(), ex);
        }

        if (wasConnected) {
            VSProtocol.LOGGER.log(Level.WARNING, "Disconnecting due to unrecoverable transport channel loss. Remote address: " + getRemoteAddress(), ex);
        } else {
            VSProtocol.LOGGER.log(Level.FINER, "Disconnecting (re-triggered) due to unrecoverable transport channel loss. Remote address: " + getRemoteAddress(), ex);
        }

        // and then notify all channels that we lost transport
        IOException iex = ex instanceof IOException ? (IOException)ex : null;
        synchronized (channels) {
            for (VSChannelImpl vsChannel : channels) {
                if (vsChannel != null) {
                    // TODO: Review. Calling onDisconnected while holding "channels" lock may lead to deadlocks
                    vsChannel.onDisconnected(iex);
                }
            }
        }

        if (triggerClose) {
            // notify state listener that connections lost
            if (stateListenerCopy != null) {
                if (VSProtocol.LOGGER.isLoggable(Level.FINER)) {
                    VSProtocol.LOGGER.log(Level.FINER, "Notifying state listener about disconnection. Remote address: " + getRemoteAddress());
                }
                stateListenerCopy.onDisconnected();
            }

            close();
        } else {
            // If this thread is not responsible for closing dispatcher,
            // just wait until dispatcher gets closed by other thread.
            boolean success;
            try {
                success = closeLatch.await(lingerInterval + 1_000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed waiting for dispatcher to close after transport recovery failure.", e);
            }
            if (!success) {
                VSProtocol.LOGGER.log(Level.WARNING, "Timeout waiting for dispatcher to close after transport recovery failure. Remote address: " + getRemoteAddress());
                // No other thread closed the dispatcher in a timely manner, close it ourselves. Even if it may break order between .onDisconnected() and .disposed() events.
                close();
            }
        }
    }

    boolean isShutdownState() {
        VSDispatcherState value = state.get();
        return value == VSDispatcherState.DISCONNECTING || value == VSDispatcherState.DISCONNECTED;
    }

    /**
     * Return true, if it has CONNECTED state.
     * Return false, if it has INITIAL, DISCONNECTED or DISCONNECTING state.
     * Otherwise, waits at least {@link #lingerInterval} until status gets CONNECTED or DISCONNECTED.
     *
     * @return true if connected, false if disconnected
     */
    public boolean tryGetConnectionStatus() {
        // Get current phase before checking state
        int phase = recoveringTransports.getPhase();

        // Read current status
        switch (state.get()) {
            case CONNECTED:
                return true;
            case INITIAL:
            case DISCONNECTED:
            case DISCONNECTING:
                return false;
            case RECONNECTING:
                // Wait below
        }

        // What for the phase to change
        recoveringTransports.awaitAdvance(phase);

        // Check new status
        switch (state.get()) {
            case CONNECTED:
                return true;
            case INITIAL:
            case DISCONNECTED:
            case DISCONNECTING:
                return false;
            case RECONNECTING:
            default: {
                // Special case: we are still in reconnection state, even after phase advanced.
                if (recoveringTransports.isTerminated()) {
                    return false;
                }
                // It may be possible that the waiting transport count was just decremented to zero
                // but state was not updated yet. Check that.
                return recoveringTransports.getUnarrivedParties() == 0;
            }
        }
    }

    /**
     * Waits for the specified channel to become checked in.
     *
     * @param channel channel to wait for
     * @param now current time
     * @param endTime completion deadline (will stop after this time even if channel still checked out)
     * @return true if channel was checked in
     */
    @GuardedBy("transportChannels")
    private boolean waitForTransportCheckIn(VSTransportChannel channel, long now, long endTime) {
        assert Thread.holdsLock(transportChannels);

        boolean checkedIn = false;
        try {
            while (now < endTime && !checkedIn && !isShutdownState()) {
                transportChannels.wait(endTime - now);
                now = System.currentTimeMillis();
                synchronized (freeChannels) {
                    checkedIn = !channel.checkedOut;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (VSProtocol.LOGGER.isLoggable(Level.FINE)) {
                VSProtocol.LOGGER.log(Level.FINE, "Error waiting to reconnect.", e);
            }
        }
        return checkedIn;
    }

    public void                         closeTransport() throws IOException, InterruptedException {
        synchronized (transportChannels) {
            Iterator<VSTransportChannel> iterator = transportChannels.iterator();

            if (iterator.hasNext()) {
                VSTransportChannel next = iterator.next();
                next.socket.close();
            }
        }
    }

    void                                checkIn (VSTransportChannel tc) {
        synchronized (transportChannels) {
            if (transportChannels.contains(tc)) {
                synchronized (freeChannels) {
                    tc.checkedOut = false;
                    freeChannels.add (tc);
                    freeChannels.notify();
                }
            } else {
                // This was removed from dispatcher, possibly channel recovery in progress.
                synchronized (freeChannels) {
                    tc.checkedOut = false;
                }
                // Notifies thread that waits in {@link #transportStopped(VSTransportChannel, Throwable)}.
                transportChannels.notifyAll();
                VSProtocol.LOGGER.log (Level.INFO, "Adding closed channel - ignored.");
            }
        }
    }

    VSTransportChannel          checkOut ()
            throws InterruptedException, ConnectionAbortedException
    {
        synchronized (freeChannels) {
            for (;;) {
                if (isShutdownState() && !remoteConnected) {
                    throw new ConnectionAbortedException("Connection aborted from remote side [" + getRemoteAddress() + "]");
                }

                if (!freeChannels.isEmpty ()) {
                    VSTransportChannel channel = freeChannels.pop();
                    channel.checkedOut = true;
                    return channel;
                }

                freeChannels.wait ();
            }
        }
    }

    private int                 getActiveChannels() {
        synchronized (channels){
            return activeChannels;
        }
    }

    public void                 close(boolean wait) {
        if (wait) {
            if (getActiveChannels() > 0)
                synchronized (channels) {
                    try {
                        channels.wait(SHUTDOWN_TIMEOUT);
                    } catch (InterruptedException e) {
                        if (VSProtocol.LOGGER.isLoggable(Level.FINE))
                            VSProtocol.LOGGER.log (Level.FINE, "Error waiting to shutdown", e);
                    }
                }
        }

        if (wait && getActiveChannels() > 0) {
//            synchronized (channels) {
//                for (VSChannelImpl channel : channels) {
//                    if (channel != null)
//                        System.out.println(channel);
//                }
//            }
            VSProtocol.LOGGER.log(Level.INFO, "Disconnect by timeout having opened " + getActiveChannels() + " channels");
        }

        close();
    }

    void                        onRemoteClosed() {
        remoteConnected = false;

        // notify all waiting threads in checkOut()
        synchronized (freeChannels) {
            freeChannels.notifyAll();
        }
    }

    private void                sendClosing() {
        // TODO: In theory we can try to check if there are any free transport channels and try to use them to send
        //  the close message. But in practice, if we are not connected anymore, then we are not very likely to succeed.
        if (!remoteConnected || state.get() != VSDispatcherState.CONNECTED)
            return;

        VSTransportChannel    channel = null;
        try {
            byte[] buffer = new byte[2];
            DataExchangeUtils.writeUnsignedShort(buffer, 0, DISPATCHER_CLOSE);
            channel = checkOut();
            channel.write(buffer);
        } catch (Exception e) {
            VSProtocol.LOGGER.log (Level.INFO, "Error sending dispatcher close");
        } finally {
            if (channel != null)
                checkIn (channel);
        }
    }

    @Override
    public void                 close () {

        sendClosing();

        // Change state to DISCONNECTING if it was not DISCONNECTED already.
        // This state change disables triggering of stateListener.onDisconnected() on transport channel error.
        // So normal dispatcher.close() will not trigger onDisconnected() event.
        state.getAndUpdate(prevState -> {
            if (prevState == VSDispatcherState.DISCONNECTED) {
                return VSDispatcherState.DISCONNECTED;
            } else {
                return VSDispatcherState.DISCONNECTING;
            }
        });

        synchronized (transportChannels) {
            for (VSTransportChannel tc : transportChannels)
                Util.close (tc);

            transportChannels.clear ();
            transportChannels.notify();
        }

        remoteConnected = false;

        // disable free channels to prevent locking on code below
        synchronized (freeChannels) {
            freeChannels.clear ();
            freeChannels.notifyAll ();
        }

        // notify channels that no transport available
        VSChannel[] virtualChannels = getVirtualChannels();
        for (VSChannel vsChannel : virtualChannels)
            if (vsChannel != null)
                ((VSChannelImpl)vsChannel).onDisconnected(null);

        synchronized (channels) {
            channels.clear();
        }

        TimerTask task = flusher;
        if (task != null)
            task.cancel();
        flusher = null; // for GC

        Timer t = timer;
        if (t != null)
            t.cancel(); // stop timer thread
        timer = null; // for GC

        VSDispatcherState prevState = state.getAndUpdate(x -> VSDispatcherState.DISCONNECTED);
        if (prevState != VSDispatcherState.DISCONNECTED) {
            // This may be triggered only once per dispatcher lifetime
            notifyDisposedEventListeners();
        }

        recoveringTransports.forceTermination();

        closeLatch.countDown();
    }

    VSChannelImpl               newChannel (int inCapacity, int outCapacity, boolean compressed) {
        VSChannelImpl               vsc;

        if (!remoteConnected) {
            throw new IllegalStateException("Attempt to create new channel after disconnect");
        }

        synchronized (channels) {
            if (activeChannels >= channelsLimit && channelsLimit > 0)
                throw new IllegalStateException("Attempt to create new channel above channels limit = " + channelsLimit);

            int                     localId = channels.indexOf (null);
            boolean                 extend = localId < 0;

            if (extend)
                localId = channels.size ();

            if (localId >= LISTENER_ID)
                throw new IllegalStateException ("Too many channels are open");

            index += isClient ? -1 : 1;
            vsc = new VSChannelImpl (this, inCapacity, outCapacity, compressed, localId, index, contextContainer);

            if (extend) {
                channels.add (vsc);
            } else {
                assert (channels.get(localId) == null);
                channels.set (localId, vsc);
            }

            activeChannels++;
            channels.notify();
        }

        return (vsc);
    }

    public VSChannel []         getVirtualChannels () {
        synchronized (channels) {
            //noinspection ToArrayCallWithZeroLengthArrayArgument
            return (channels.toArray (new VSChannel [channels.size ()]));
        }
    }

    public String               getRemoteAddress() {
        return address;
    }

    public String               getClientAddress() {
        return "/" + clientAddress + ":";
    }

    VSChannelImpl               getChannel (int id) {
        synchronized (channels) {
            if (id >= channels.size() ) {
                // Client requested a channel that never existed
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning("Attempt to use invalid channel id: " + id + ", max valid value: " + (channels.size() - 1));
                }
                return null;
            }

            return (channels.get (id));
        }
    }

    long                        getLatency() {
        VSTransportChannel tc = null;
        try {
            return (tc = checkOut()).getLatency();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } catch (ConnectionAbortedException e) {
            return 0;
        } finally {
            if (tc != null)
                checkIn(tc);
        }
    }

    void                        channelClosed (VSChannelImpl vsc) {
        int id = vsc.getLocalId();

        synchronized (channels) {
            if (vsc.equals(channels.get(id))) {
                channels.set(id, null);

                activeChannels--;
                channels.notify();
            } else {
                VSProtocol.LOGGER.log(Level.SEVERE, "Trying to remove wrong channel.");
            }
        }
    }

    public void                     addDisposableListener(DisposableListener<VSDispatcher> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void                     removeDisposableListener(DisposableListener<VSDispatcher> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    private DisposableListener<VSDispatcher>[]    getListeners() {
        DisposableListener<VSDispatcher>[] list;

        synchronized (listeners) {
            //noinspection ToArrayCallWithZeroLengthArrayArgument
            list = listeners.toArray(new DisposableListener[listeners.size()]);
        }

        return list;
    }

    private void notifyDisposedEventListeners() {
        DisposableListener<VSDispatcher>[] list = getListeners();

        for (var aList : list) {
            aList.disposed(this);
        }
    }

    public QuickExecutor getQuickExecutor() {
        return contextContainer.getQuickExecutor();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " for clientId='" + clientId;
    }
}
