/*
 * Copyright 2021 EPAM Systems, Inc
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
import com.epam.deltix.util.time.TimerRunner;
import com.epam.deltix.util.memory.DataExchangeUtils;

import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.io.*;
import java.net.SocketException;

/**
 *
 */
public final class VSDispatcher implements Disposable {

    private final ContextContainer contextContainer;
    private final ThreadFactory                     transportChannelThreadFactory;


    private final Date                              creationDate = new Date ();
    private Timer                                   timer;

    private final ObjectHashSet<VSTransportChannel> transportChannels =
        new ObjectHashSet<> ();

    private final Stack <VSTransportChannel>        freeChannels =
        new Stack <> ();

    private final ArrayList <VSChannelImpl>         channels =
            new ArrayList <> (10);
    private volatile boolean                        hasAvailableTransport = false;
    
    volatile VSConnectionListener                   connectionListener = null;
    volatile ConnectionStateListener                stateListener;

    private int                                     activeChannels;
    private int                                     reconnectInterval;

    private String                                  address;
    private String                                  applicationID;

    // State of Dispatcher on remote side 
    private volatile boolean                        remoteConnected = true;
    private volatile long                           throughput = 0;
    private volatile long                           totalBytes = 0; // number of bytes sent
    private final EMA                               average = new EMA(1000 * 60); // 1 minute

    private final AtomicBoolean                     disposed = new AtomicBoolean(false);

    private TimerTask flusher = new TimerRunner() {
        private VSChannelImpl[]         list = new VSChannelImpl[10];
        private VSTransportChannel[]    transports = new VSTransportChannel[5];
        private long                    runs = 0;

        @Override
        protected void runInternal() {

            int size = 0;
            synchronized (channels) {
                if ((size = channels.size()) > 0)
                    list = channels.toArray(list);
            }

            for (int i = 0; i < size; i++) {
                VSChannelImpl channel = list[i];
                try {
                    if (channel != null && channel.isAutoflush()) {
                        channel.getOutputStream().flushAvailable();
                    }
                } catch (ChannelClosedException e) {
                    // ignore
                } catch (ConnectionAbortedException e) {
                    VSProtocol.LOGGER.log (Level.WARNING, "Client unexpectedly drop connection. Remote address: " + channel.getRemoteAddress());
                } catch (com.epam.deltix.util.io.UncheckedIOException | IOException e ) {
                     VSProtocol.LOGGER.log (Level.WARNING, "Exception while flushing data. Remote address: " + channel.getRemoteAddress(), e);
                }
            }

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
        }
    };

    private final String        clientId;
    private final boolean       isClient;
    private volatile int        index = 0;

    private final HashSet<DisposableListener> listeners =
        new HashSet<DisposableListener> ();

    /**
     *  Constructs a dispatcher instance for the specified client.
     */
    public VSDispatcher(String clientId, boolean isClient, ContextContainer contextContainer) {
        this.clientId = clientId;
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
        return reconnectInterval;
    }

    public String               getApplicationID() {
        if (applicationID == null) {
            String[] parts = clientId.split(":");
            applicationID = parts.length == 4 ? parts[2] : "<none>";
        }
        return applicationID;
    }

    public void                 setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public void                 setLingerInterval(int reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
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
            return transportChannels.size() > 0;
        }
    }

    public boolean              hasAvailableTransport() {
        return hasAvailableTransport;
    }

    public void                 addTransportChannel (VSocket socket)
        throws IOException
    {
        boolean hasTransport = hasAvailableTransport;

        VSTransportChannel          tc = new VSTransportChannel(this, socket, transportChannelThreadFactory);
        tc.checkedOut = true; // Initially this channel is not in "freeChannels" so it is effectively "checked out"

        // set that we have transport before starting transport channel thread
        if (!hasTransport)
            hasAvailableTransport = true;

        // start transport
        tc.start ();
        
        synchronized (transportChannels) {

            if (address == null)
                address = socket.getRemoteAddress();

            transportChannels.add (tc);
            transportChannels.notify();
        }

        checkIn(tc);

        if (!hasTransport && stateListener != null)
            stateListener.onReconnected();
    }

    public void                 setConnectionListener (VSConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public void                 setStateListener(ConnectionStateListener stateListener) {
        this.stateListener = stateListener;
    }

    void                        transportStopped (VSTransportChannel channel, Throwable ex) {
        IOException iex = ex instanceof IOException ? (IOException)ex : null;

        Level disconnectLogLevel = ex instanceof EOFException ? Level.FINE : Level.INFO;
        if (VSProtocol.LOGGER.isLoggable(disconnectLogLevel)) {
            VSProtocol.LOGGER.log(disconnectLogLevel, "Transport channel has stopped. Remote address: " + channel.socket.getRemoteAddress() + ". Error: " + ex.getClass().getSimpleName() + ". Message: " + ex.getMessage());
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + reconnectInterval;

        boolean transportIsUnrecoverablyBroken = false;

        boolean wasCheckedIn;
        synchronized (transportChannels) {
            if (transportChannels.isEmpty()) // already closed
                return;

            if (!transportChannels.remove (channel)) // check that channel already removed
                return;


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

            hasAvailableTransport = transportChannels.size() > 0;
        }

        VSocketRecoveryInfo recoveryInfo = new VSocketRecoveryInfo(channel.socket, startTime);

        long now = System.currentTimeMillis();
        if (wasCheckedIn && (now < endTime)) {
            // notify state listener that transport lost
            if (stateListener != null) {
                if (stateListener.onTransportStopped(recoveryInfo)) {
                    transportIsUnrecoverablyBroken = true;
                }
            }

            if (remoteConnected && !transportIsUnrecoverablyBroken) {
                // System.out.println("WAITED: remoteConnected=" + remoteConnected + " transportIsUnrecoverablyBroken=" + transportIsUnrecoverablyBroken);
                try {
                    // Try to wait for connection restore

                    //noinspection SynchronizationOnLocalVariableOrMethodParameter
                    synchronized (recoveryInfo) {
                        long timeToWait;
                        while ((timeToWait = endTime - now) > 0 && recoveryInfo.isWaitingForRecovery() && remoteConnected) {
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

        if (stateListener != null) {
            if (stateListener.onTransportBroken(recoveryInfo)) {
                transportIsUnrecoverablyBroken = true;
            }
        }

        if (transportIsUnrecoverablyBroken) {
            boolean wasConnected = remoteConnected;

            // mark that we lost transport completely
            hasAvailableTransport = false;

            // notify all waiting for transport that connection is lost
            onRemoteClosed();

            if (ex instanceof SocketException || ex instanceof EOFException || ex instanceof SocketTimeoutException) {
                if (VSProtocol.LOGGER.isLoggable(Level.FINE))
                    VSProtocol.LOGGER.log (Level.FINE, "Exception on transport channel. Remote address: " + getRemoteAddress(), ex);
            } else {
                VSProtocol.LOGGER.log (Level.SEVERE, "Exception on transport channel. Remote address: " + getRemoteAddress(), ex);
            }
            if (wasConnected) {
                VSProtocol.LOGGER.log(Level.WARNING, "Disconnecting due to unrecoverable transport channel loss. Remote address: " + getRemoteAddress(), ex);
            } else {
                VSProtocol.LOGGER.log(Level.FINER, "Disconnecting (re-triggered) due to unrecoverable transport channel loss. Remote address: " + getRemoteAddress(), ex);
            }

            // and then notify all channels that we lost transport
            synchronized (channels) {
                for (VSChannelImpl vsChannel : channels)
                    if (vsChannel != null)
                        vsChannel.onDisconnected(iex);
            }

            // notify state listener that connections lost
            if (stateListener != null)
                stateListener.onDisconnected();

            close();
        }
    }

    /**
     * Waits for the specified channed to become checked in.
     *
     * @param channel channel to wait for
     * @param now current time
     * @param endTime completion deadline (will stop after this time even if channel still checked out)
     * @return true if channel was checked in
     */
    private boolean waitForTransportCheckIn(VSTransportChannel channel, long now, long endTime) {
        assert Thread.holdsLock(transportChannels);

        boolean checkedIn = false;
        try {
            while (now < endTime && !checkedIn) {
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

    public void                        checkIn (VSTransportChannel tc) {
        synchronized (transportChannels) {
            if (transportChannels.contains(tc)) {
                synchronized (freeChannels) {
                    tc.checkedOut = false;
                    freeChannels.add (tc);
                    freeChannels.notify();
                }
            } else {
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
                if (!hasAvailableTransport && !remoteConnected)
                    throw new ConnectionAbortedException("Connection aborted from remote side [" + getRemoteAddress() + "]");

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
        if (!remoteConnected || !hasAvailableTransport)
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

    public void                 close () {

        sendClosing();
        
        synchronized (transportChannels) {
            for (VSTransportChannel tc : transportChannels)
                Util.close (tc);

            transportChannels.clear ();
            transportChannels.notify();
        }

        remoteConnected = hasAvailableTransport = false;

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

        if (disposed.compareAndSet(false, true))
            notifyListeners();
    }

    VSChannelImpl               newChannel (int inCapacity, int outCapacity, boolean compressed) {
        VSChannelImpl               vsc;
        if (!remoteConnected) {
            throw new IllegalStateException("Attempt to create new channel after disconnect");
        }

        synchronized (channels) {
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
            if (vsc.equals(channels.get(id)))
                channels.set (id, null);
            else
                VSProtocol.LOGGER.log (Level.SEVERE, "Trying to remove wrong channel.");

            activeChannels--;
            channels.notify();
        }
    }

    public void                     addDisposableListener(DisposableListener listener) {
        synchronized (listeners) {
            if (!listeners.contains(listener))
                listeners.add(listener);
        }
    }

    public void                     removeDisposableListener(DisposableListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private DisposableListener[]    getListeners() {
        DisposableListener[] list;

        synchronized (listeners) {
            //noinspection ToArrayCallWithZeroLengthArrayArgument
            list = listeners.toArray(new DisposableListener[listeners.size()]);
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    private void                    notifyListeners() {
        DisposableListener[] list = getListeners();

        for (DisposableListener aList : list)
            aList.disposed(this);
    }

    public QuickExecutor            getQuickExecutor() {
        return contextContainer.getQuickExecutor();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) + " for clientId='" + clientId;
    }
}