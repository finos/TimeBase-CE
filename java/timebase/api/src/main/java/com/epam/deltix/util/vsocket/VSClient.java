/*
 * Copyright 2026 EPAM Systems, Inc
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

import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.offheap.OffHeap;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.lang.DisposableListener;
import com.epam.deltix.util.time.GlobalTimer;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.time.TimerRunner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;

/**
 *
 */
public class VSClient extends ConnectionStateListener implements Disposable, DisposableListener<VSDispatcher> {
    public static final int             MIN_ALLOWED_SERVER_VERSION = 1014;

    public static final int             MIN_COMP_SERVER_VERSION = VSProtocol.VERSION;
    public static final int             MAX_COMP_SERVER_VERSION = VSProtocol.VERSION;

    public static final String          SSL_TERMINATION_PROPERTY = "TimeBase.network.VSClient.sslTermination";
    public static final boolean         SSL_TERMINATION = Boolean.getBoolean(SSL_TERMINATION_PROPERTY);

    //private static final int MAX_TRANSPORT_RECONNECT_ATTEMPTS = Integer.getInteger("TimeBase.network.VSClient.maxTransportReconnectAttempts", 5);
    private final int transportReconnectAttemptInterval;
    private final int socketSendBufferSize;
    private final int socketReceiveBufferSize;

    private static final int RE_ATTEMPT_EXTRA_DELAY = 10; // Extra delay to avoid situation when we re-schedule task due to timer jitter

    private static final VSClientOptions DEFAULT_CLIENT_OPTIONS = new VSClientOptions();

    private String                      host;
    private int                         port;
    private int                         numTransportChannels = 3;

    private volatile VSDispatcher       dispatcher;
    // Protects "dispatcher" field and interactions with quick executor
    private final Object dispatcherLock = new Object();

    private String                      clientId;
    private long                        serverTime = -1;    

    private volatile DisconnectEventListener     listener;
    private int                         reconnectInterval;
    private VSCompression               serverCompression;

    private int                         soTimeout;
    private int                         timeout;

    private boolean                     enableSSL = false;
    private final boolean               sslTermination;
    private int                         sslPort = 0;
    private SSLContext sslContext;


    private final ContextContainer      contextContainer;

    private int                         protocolVersion = VSProtocol.VERSION;

    private volatile boolean closed = false;

    // Elements should be sorted (when possible) by time of last reconnection attempt however there is no strict enforcement for this.
    // New broken sockets should be added to the head of the queue.
    // Broken sockets that failed to reconnect should be added to the tail of the queue.
    private final ConcurrentLinkedDeque<VSocketRecoveryInfo> broken = new ConcurrentLinkedDeque<>();

    private final QuickExecutor.QuickTask reconnector;

    private QuickExecutor.QuickTask createReconnectorTask(final QuickExecutor quickExecutor) {
        return new QuickExecutor.QuickTask(quickExecutor) {
            @Override
            public void run() {
                for ( ;; ) {
                    long currentTime = TimeKeeper.currentTime;

                    VSocketRecoveryInfo socketRecovery = broken.peek();

                    if (dispatcher == null || socketRecovery == null)
                        break;

                    long lastReconnectAttemptTs = socketRecovery.getLastReconnectAttemptTs();
                    if (lastReconnectAttemptTs > currentTime - transportReconnectAttemptInterval) {
                        // It's too early to recover this socket
                        scheduleReconnectAttempt(lastReconnectAttemptTs + transportReconnectAttemptInterval + RE_ATTEMPT_EXTRA_DELAY);
                        return;
                    }

                    boolean recoveryAttemptEnded = false;
                    synchronized (socketRecovery) {
                        if (!socketRecovery.startRecoveryAttempt()) {
                            if (socketRecovery.isRecoveryAttemptInProgress()) {
                                throw new IllegalStateException();
                            } else {
                                // Recovery for that socket already ended
                                continue;
                            }
                        }
                    }
                    try {
                        if (!broken.remove(socketRecovery)) {
                            // The socket just was removed from broken list by other thread
                            continue;
                        }

                        VSocket socket = socketRecovery.getSocket();

                        // Start reconnect attempt
                        int attemptNumber;
                        synchronized (socketRecovery) {
                            attemptNumber = socketRecovery.addReconnectAttempt(currentTime);
                        }


                        boolean success = false;
                        boolean transportLost = false;
                        try {
                            // Try to reconnect - long operation
                            VSocket vSocket = openTransport(socket);
                            if (vSocket != null) {
                                success = true;
                                dispatcher.addTransportChannel(vSocket);
                                synchronized (socketRecovery) {
                                    if (socketRecovery.tryMarkRecoverySucceeded()) {
                                        socketRecovery.notifyAll();
                                    } else {
                                        VSProtocol.LOGGER.log(Level.WARNING, "Reconnect succeeded but recovery process is already cancelled");
                                    }
                                }
                                VSProtocol.LOGGER.log(Level.INFO, "Reconnect success, connection " + socket.getSocketIdStr() + ", address " + socket.getRemoteAddress() + " after " + attemptNumber + " attempts");
                            } else {
                                VSProtocol.LOGGER.log(Level.INFO, "Reconnect failed (no error), connection " + socket.getSocketIdStr() + ", address " + socket.getRemoteAddress() + ", attempt " + attemptNumber);
                            }
                        } catch (ConnectionRejectedException e) {
                            // Explicit reject from server. That means that we should not try to reconnect anymore.
                            transportLost = true;
                            VSProtocol.LOGGER.log(Level.INFO, "Reconnect rejected by server, connection " + socket.getSocketIdStr() + ", address " + socket.getRemoteAddress() + ", attempt " + attemptNumber);
                        } catch (IOException e) {
                            VSProtocol.LOGGER.log(Level.INFO, "Reconnect failed (" + e.getMessage() + "), connection " + socket.getSocketIdStr() + ", address " + socket.getRemoteAddress() + ", attempt " + attemptNumber);
                        } catch (TransportRecoveryFailre transportRecoveryFailre) {
                            transportLost = true;
                        }

                        if (!success) {
                            if (currentTime >= socketRecovery.getRecoveryDeadlineTs()) {
                                // At this time socket is discarded on the server side so we should give up now
                                VSProtocol.LOGGER.log(Level.WARNING, "Transport " + socket.getSocketIdStr() + " was not recovered after " + attemptNumber + " attempts (timeout reached)");
                                transportLost = true;
                            }
                            if (transportLost) {
                                // We failed to recover the connection so we have to disconnect entire transport because we might loss some data
                                synchronized (socketRecovery) {
                                    socketRecovery.stopRecoveryAttempt();
                                    assert !socketRecovery.isRecoverySucceeded();
                                    socketRecovery.markRecoveryFailed();
                                    socketRecovery.notifyAll();
                                }
                                recoveryAttemptEnded = true;
                                //VSProtocol.LOGGER.log(Level.WARNING, "Disconnecting client due to failure to recover transport channel " + socket.getSocketIdStr() + " after " + attemptNumber + " attempts");

                                //VSClient.this.close(false);
                                return;
                            } else {
                                // We failed to recover but we can try again later
                                // Add to the last position so it will be last to try
                                broken.addLast(socketRecovery);
                            }
                        }

                    } finally {
                        if (!recoveryAttemptEnded) {
                            synchronized (socketRecovery) {
                                socketRecovery.stopRecoveryAttempt();
                                socketRecovery.notifyAll();
                            }
                        }
                    }
                }
            }

            @Override
            protected boolean killSupported() {
                return true;
            }
        };
    }

    private void scheduleReconnectAttempt(long nextAttemptTimestamp) {
        GlobalTimer.INSTANCE.schedule(new TimerRunner() {
            @Override
            public void runInternal() {
                reconnector.submit();
            }
        }, new Date(nextAttemptTimestamp));
    }

    @VisibleForTesting // Should by used in tests ONLY. TODO: Delete?
    public VSClient (String host, int port, String ownerID) throws IOException {
        this(host, port, ownerID, false, ContextContainer.getContextContainerForClientTests());
    }

    @VisibleForTesting // Should by used in tests ONLY. TODO: Create a factory method with name like "createClientForTests"
    public VSClient (String host, int port) throws IOException {
        this(host, port, null, false, ContextContainer.getContextContainerForClientTests());
    }

    public VSClient(String host, int port, @Nullable String ownerID, boolean enableSSL, ContextContainer contextContainer) throws IOException {
        this(host, port, ownerID, enableSSL, SSL_TERMINATION, contextContainer);
    }

    public VSClient(String host, int port, @Nullable String ownerID, boolean enableSSL, boolean sslTermination,
                    ContextContainer contextContainer) throws IOException {
        this(host, port, ownerID, enableSSL, contextContainer, withSslTermination(sslTermination));
    }

    @ApiStatus.Experimental
    public VSClient(String host, int port, @Nullable String ownerID, boolean enableSSL,
                    ContextContainer contextContainer, VSClientOptions clientOptions) throws IOException {
        this.host = host;
        this.port = port;
        this.enableSSL = enableSSL;
        this.sslTermination = clientOptions.isSslTermination();
        this.contextContainer = contextContainer;
        this.reconnector = createReconnectorTask(contextContainer.getQuickExecutor());

        if (ownerID == null)
            this.clientId = new GUID().toStringWithPrefix (InetAddress.getLocalHost().getHostAddress() + ":");
        else
            this.clientId = new GUID().toStringWithPrefix(InetAddress.getLocalHost().getHostAddress() + ":" + ownerID + ":");

        this.transportReconnectAttemptInterval = clientOptions.getTransportReconnectAttemptInterval();
        this.soTimeout = clientOptions.getHandshakeSocketTimeout();
        this.timeout = clientOptions.getSocketConnectTimeout();
        this.socketSendBufferSize = clientOptions.getSocketSendBufferSize();
        this.socketReceiveBufferSize = clientOptions.getSocketReceiveBufferSize();
    }

    public void                     setClientAddress(String address, String ownerID) {
        this.clientId = new GUID().toStringWithPrefix(address + ":" + ownerID + ":");
    }

    public int                      getSoTimeout () {
        return soTimeout;
    }

    public void                     setSoTimeout (int soTimeout) {
        this.soTimeout = soTimeout;
    }

    public int                      getTimeout() {
        return timeout;
    }

    public void                     setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String                   getHost () {
        return host;
    }

    public void                     setHost (String host) {
        this.host = host;
    }

    public int                      getNumTransportChannels () {
        return numTransportChannels;
    }

    public void                     setNumTransportChannels (int numChannels) {
        numTransportChannels = numChannels;
    }

    public int                      getPort () {
        return port;
    }

    public void                     setPort (int port) {
        this.port = port;
    }

    public void                     setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public long                     getServerStartTime() {
        return serverTime;
    }

    public int                      getReconnectInterval() {
        return reconnectInterval;
    }

    /**
     * Checks if client is connected.
     * Will not wait but will return true even if reconnecting and there is no immediately available transports.
     *
     * <p>It returns true during reconnecting phase because it would be inconsistent to return false,
     * considering that reconnecting state does not trigger "disconnected" event.
     *
     * <p>In most cases you should use {@link #tryGetConnectionStatus()} instead.
     *
     * @return true if connected or reconnecting, false otherwise
     */
    public boolean                  isConnected() {
        return dispatcher != null && dispatcher.isConnectedOrReconnecting();
    }

    /**
     * Checks if client is fully connected right now.
     * Will not wait and will return false if reconnecting.
     *
     * @return true if connected and NOT reconnecting, false otherwise
     */
    public boolean isConnectedAndNotReconnecting() {
        return dispatcher != null && dispatcher.isConnectedAndNotReconnecting();
    }

    /**
     * Return true, if it has CONNECTED state.
     * Return false, if it has DISCONNECTED/DISCONNECTING state.
     * Otherwise, waits until status gets CONNECTED or DISCONNECTED.
     *
     * @return true if connected, false if disconnected
     */
    public boolean                  tryGetConnectionStatus() {
        if (dispatcher == null) {
            return false;
        } else {
            return dispatcher.tryGetConnectionStatus();
        }
    }

    public void                     connect () throws IOException {
        if (dispatcher != null)
            throw new IllegalStateException("Already connected");

        VSocket socket = openTransport(); // try to connect

        synchronized (dispatcherLock) {
            contextContainer.getQuickExecutor().reuseInstance();

            dispatcher = new VSDispatcher(clientId, true, contextContainer);
            dispatcher.setLingerInterval(reconnectInterval);
            dispatcher.addDisposableListener(this);
        }
        dispatcher.addTransportChannel (socket);

        for (int ii = 1; ii < numTransportChannels; ii++)
            dispatcher.addTransportChannel (openTransport());

        dispatcher.setStateListener(this);
    }

    public VSDispatcher         getDispatcher() {
        return dispatcher;
    }

    public                          void increaseNumTransportChannels() throws IOException {
        numTransportChannels++;

        if (dispatcher != null)
            dispatcher.addTransportChannel (openTransport ());
    }

    @NotNull
    private Socket setupSocket() throws IOException {
        // If SSL termination is enabled, then we start with SSL socket and will NOT try to perform upgrade.
        // This is necessary because intermediate proxies will get confused
        // if we start with non-SSL socket and then upgrade to SSL after negotiation with TB.
        boolean startWithSSL = enableSSL && sslTermination;

        InetSocketAddress socketAddress = new InetSocketAddress(host, port);

        Socket socket = null;
        boolean success = false;
        try {
            if (startWithSSL) {
                VSProtocol.LOGGER.info("SSL termination enabled: creating SSL socket on [" + host + ":" + port + "]");
                socket = sslContext.getSocketFactory().createSocket();
            } else {
                socket = new Socket();
            }

            socket.setSoTimeout(soTimeout);
            socket.setTcpNoDelay(true);

            // Sets socket buffer sizes.
            // It's important to configure receive buffer size before connection is established
            // to allow it to use TCP window size greater than 64kb.
            // That's why we have to do that here.
            socket.setReceiveBufferSize(this.socketReceiveBufferSize);
            socket.setSendBufferSize(this.socketSendBufferSize);

            // Connect
            socket.connect(socketAddress, timeout);

            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            // We should not request SSL from TB server if SSL termination is enabled
            boolean requestSSL = enableSSL && !sslTermination;

            os.write(0); //first byte of VS protocol
            os.write(VSProtocol.getHeader(requestSSL));
            os.flush();

            int serverResponse = is.read();
            if (serverResponse == VSProtocol.CONN_RESP_SSL_NOT_SUPPORTED) {
                assert !startWithSSL;
                throw new IOException("Server not supported SSL.");
            } else if (serverResponse != VSProtocol.CONN_RESP_OK) {
                throw new RuntimeException("Unexpected server response: " + serverResponse);
            }

            int serverHeader = is.read();
            if (serverHeader == VSProtocol.SSL_HEADER) {

                if (startWithSSL) {
                    throw new IllegalStateException("SSL termination is enabled but server attempts to upgrade to SSL");
                }

                // Upgrade non-SSL socket to SSL
                socket = sslContext.getSocketFactory().createSocket(
                        socket, socket.getInetAddress().getHostAddress(), socket.getPort(), true);
                ((SSLSocket) socket).setUseClientMode(true);
                ((SSLSocket) socket).startHandshake();
                enableSSL = true; // We now use SSL socket, even if client have not requested it.
                VSProtocol.LOGGER.info("Socket upgraded to SSL socket! Now connection is secured.");
            } else if (serverHeader == VSProtocol.HEADER) {
                if (enableSSL && !startWithSSL) {
                    // Normally we should not get here:
                    // 1. If SSL termination is enabled, then we don't request SSL from TB server
                    // 2. If we have enableSSL = true, then we should request it from TB server and get explicit reject if it's not supported
                    VSProtocol.LOGGER.info("Connection isn't secured.");
                    enableSSL = false;
                }
            } else {
                throw new RuntimeException("Unexpected server header: " + serverHeader);
            }

            success = true;
            return socket;
        } finally {
            if (!success) {
                IOUtil.close(socket);
            }
        }
    }

    public void             setProtocolVersion(int version) {
        if (version < VSClient.MIN_ALLOWED_SERVER_VERSION || version > VSClient.MAX_COMP_SERVER_VERSION)
            throw new IllegalArgumentException("Protocol version should be in range [" +
                    VSClient.MIN_ALLOWED_SERVER_VERSION + ", " + VSClient.MAX_COMP_SERVER_VERSION + "]");

        this.protocolVersion = version;
    }

    /** Used for re-connecting existing VSocket */
    @SuppressFBWarnings(value = "UNENCRYPTED_SOCKET", justification = "Timebase ports should be protected from public access by SSL-terminating NLB")
    private VSocket                         openTransport (VSocket stopped) throws IOException, TransportRecoveryFailre {
        Socket              s = null;
        boolean             ok = false;

        try {
            s = setupSocket();

            ClientConnection cc = new ClientConnection(s);

            DataOutputStream    dos = new DataOutputStream (cc.getOutputStream());
            DataInputStream     dis = new DataInputStream (cc.getBufferedInputStream());

            //check version compatibility
            dos.writeInt (protocolVersion);
            dos.writeUTF(clientId);
            dos.flush();

            int spv = dis.readInt ();

            String sid = dis.readUTF ();
            if (spv != protocolVersion && (spv < MIN_COMP_SERVER_VERSION || spv > MAX_COMP_SERVER_VERSION))
                throw new IncompatibleClientException (sid, spv);

            sslPort = dis.readInt();

            if (protocolVersion > 1014)
                processTransportHandshake(dis);

            //send other sync data
            dos.writeBoolean(false); // restore
            dos.writeInt (stopped.getCode()); // socket id
            dos.writeLong(stopped.getInputStream().getBytesRead()); // number of read bytes
            dos.flush ();

            s.setSoTimeout(0);

            int                 resp = dis.readByte ();

            if (resp != VSProtocol.CONN_RESP_OK) {
                throw new ConnectionRejectedException (sid, resp);
            } else {
                long time = dis.readLong();
                if (serverTime != -1 && serverTime != time)
                    throw new ServerRestartedException(sid, time);
                else
                    serverTime = time;

                this.reconnectInterval = dis.readInt();

                String compression = dis.readUTF();
                this.serverCompression = Enum.valueOf(VSCompression.class, compression);

                long numBytesRecieved = dis.readLong(); // number of bytes recieved by remote side
                ok = numBytesRecieved != -1;

                if (ok) {
                    stopped.getOutputStream().confirm(numBytesRecieved);
                    return VSocketFactory.get(cc, stopped);
                } else {
                    // TODO: We have to close client here
                    if (VSProtocol.LOGGER.isLoggable(Level.WARNING)) {
                        boolean hadUnconfirmedData = stopped.getOutputStream().hasUnconfirmedData();
                        String transportTag = stopped.getCode() + " / " + Integer.toHexString(stopped.getCode());
                        VSProtocol.LOGGER.warning("Failed to restore transport " + transportTag + ". Data loss: " + (hadUnconfirmedData ? "yes" : "uncertain"));
                    }
                    throw new TransportRecoveryFailre();
                }
            }

        } finally {
            if (!ok)
                IOUtil.close (s);
        }
    }

    @SuppressFBWarnings(value = "UNENCRYPTED_SOCKET", justification = "Timebase ports should be protected from public access by SSL-terminating NLB")
    VSocket                             openTransport () throws IOException {
        Socket              s = null;
        boolean             ok = false;
        TransportType transportType;
        ClientConnection cc;

        try {
            s = setupSocket();
            cc = new ClientConnection(s);

            DataOutputStream    dos = new DataOutputStream (cc.getOutputStream());
            DataInputStream     dis = new DataInputStream (cc.getBufferedInputStream());

            //check version compatibility
            dos.writeInt (protocolVersion);
            dos.writeUTF(clientId);
            dos.flush ();
            int spv = dis.readInt ();
            String sid = dis.readUTF ();

            if (spv != protocolVersion && (spv < MIN_COMP_SERVER_VERSION || spv > MAX_COMP_SERVER_VERSION))
                throw new IncompatibleClientException (sid, spv);

            sslPort = dis.readInt();

            transportType = (protocolVersion > 1014) ? processTransportHandshake(dis) : TransportType.SOCKET_TCP;

            //send other sync data
            dos.writeBoolean(true);
            dos.writeInt(s.hashCode()); // socket id
            dos.writeLong(0L); // number of read bytes
            dos.flush ();

            s.setSoTimeout (0);

            int                 resp = dis.readByte ();

            if (resp != VSProtocol.CONN_RESP_OK) {
                throw new ConnectionRejectedException (sid, resp);
            } else {
                long time = dis.readLong();
                if (serverTime != -1 && serverTime != time)
                    throw new ServerRestartedException(sid, time);
                else
                    serverTime = time;

                this.reconnectInterval = dis.readInt();

                String compression = dis.readUTF();
                long numBytesRecieved = dis.readLong();
                assert numBytesRecieved == 0; // new connections should have = 0;
                this.serverCompression = Enum.valueOf(VSCompression.class, compression);
            }
            
            ok = true;
        } finally {
            if (!ok)
                IOUtil.close (s);
        }

        return VSocketFactory.get(cc, transportType);
    }

    private TransportType           processTransportHandshake(DataInputStream dis) throws IOException {
        TransportType transportType = TransportType.values()[dis.readInt()];
        if (transportType == TransportType.AERON_IPC) {
            throw new RuntimeException("Legacy version of Aeron IPC is not supported");
        } else if (transportType == TransportType.OFFHEAP_IPC) {
            OffHeap.start(dis.readUTF(), false);
        }

        return transportType;
    }

    public VSChannel                openChannel () throws IOException {
        return openChannel(VSProtocol.CHANNEL_BUFFER_SIZE, VSProtocol.CHANNEL_BUFFER_SIZE, false);
    }

    public VSChannel                openChannel (int inCapacity, int outCapacity, boolean compressed) throws IOException {
        if (serverCompression == VSCompression.OFF)
            compressed = false;
        else if (serverCompression == VSCompression.ON)
            compressed = true;

        VSChannelImpl   vsc = dispatcher.newChannel (inCapacity, outCapacity, compressed);

        try {
            vsc.sendConnect ();
        } catch (InterruptedException x) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException ();
        }

        return (vsc);
    }

    @Override
    public void                     close () {
        close(true);
    }

    private void close(boolean waitForChannelsToFinish) {
        boolean triggerDisconnectEvent;
        synchronized (dispatcherLock) {
            closed = true;

            VSDispatcher d = dispatcher;

            // If dispatcher is null, then we already disconnected or even never were connected.
            // If dispatcher is in shutdown state, then disconnect event already was triggered.
            // Note that this check does not give 100% guarantee that disconnect event will be triggered no more than once
            // because of race between checking isShutdownState() and calling d.setStateListener(null).
            // However, in practice this should be sufficient.
            triggerDisconnectEvent = d != null && !d.isShutdownState();

            if (d != null) {
                d.setStateListener(null);
                d.removeDisposableListener(this);
                d.close(waitForChannelsToFinish);

                contextContainer.getQuickExecutor().shutdownInstance();
            }

            dispatcher = null;
        }
        // Trigger a disconnect event, so any disconnect listeners can be notified.
        // https://gitlab.deltixhub.com/Deltix/QuantServer/QuantServer/-/issues/1269
        // However, this also results that onDisconnect event will be triggered even if no "unexpected disconnect" actually happened.
        // So while VSDispatcher does not trigger disconnect event if it shut down gracefully, VSClient.close() will still trigger it.
        // TODO: Decide if we want to call .onDisconnected() in case of normal shutdown.
        // TODO: This should be reviewed after TickDBClient refactor. We may want to completely remove this call
        //  as updated VSDispatcher already triggers disconnect event on unexpected disconnects
        //  and state change that is caused by TickDBClient closing the connection may be handled in TickDBClient itself.
        if (triggerDisconnectEvent) {
            DisconnectEventListener listenerRef = listener;
            if (listenerRef != null) {
                listenerRef.onDisconnected();
            }
        }
    }

    public void                     setDisconnectedListener(DisconnectEventListener listener) {
        this.listener = listener;
    }

    @Override
    boolean onTransportRecoveryStart(VSocketRecoveryInfo recoveryInfo) {
        if (dispatcher != null) {
            // TODO: Ensure that we can't get duplicate instance of socket in the broken list
            broken.addFirst(recoveryInfo);

            reconnector.submit();
            return false;
        } else {
            return true;
        }
    }

    @Override
    boolean onTransportRecoveryStop(VSocketRecoveryInfo recoveryInfo) {
        try {
            synchronized (recoveryInfo) {
                while (recoveryInfo.isRecoveryAttemptInProgress()) {
                    recoveryInfo.wait();
                }
                if (!recoveryInfo.isRecoveryEnded()) {
                    recoveryInfo.markRecoveryFailed();
                }
                boolean removed = broken.remove(recoveryInfo);
                if (!removed && !recoveryInfo.isRecoverySucceeded()) {
                    VSProtocol.LOGGER.warning("Transport for " + recoveryInfo.getSocket().getCode() + " is missing from broken transport list");
                }

                boolean recoveryFailed = recoveryInfo.isRecoveryFailed();
                if (recoveryFailed) {
                    if (VSProtocol.LOGGER.isLoggable(Level.FINE)) {
                        VSProtocol.LOGGER.fine("Transport for " + recoveryInfo.getSocket().getCode() + " was permanently lost");
                    }
                }
                return recoveryFailed || (!removed && !recoveryInfo.isRecoverySucceeded());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    @Override
    void onConnected() {
        if (listener != null)
            listener.onReconnected();
    }

    @Override
    void                            onDisconnected() {
        reconnector.kill();

        if (listener != null)
            listener.onDisconnected();
    }

    public  long                    getLatency() {
        return dispatcher != null ? dispatcher.getLatency() : Long.MAX_VALUE;
    }

    @Override
    public void                     disposed (VSDispatcher d) {
        synchronized (dispatcherLock) {
            closed = true;

            if (d == dispatcher) {
                d.setStateListener(null);
                d.removeDisposableListener(this);
                contextContainer.getQuickExecutor().shutdownInstance();

                dispatcher = null;
            }
        }
    }

    public boolean                  isSSLEnabled() {
        return enableSSL;
    }

    public int                      getSSLPort() {
        return sslPort;
    }

    @Override
    public String                   toString () {
        return ("VSClient (" + host + ":" + port + ")");
    }

    /** Returns default client options based on system properties, with "sslTermination" set to provided value */
    private static VSClientOptions withSslTermination(boolean sslTermination) {
        if (sslTermination == DEFAULT_CLIENT_OPTIONS.isSslTermination()) {
            return DEFAULT_CLIENT_OPTIONS;
        } else {
            VSClientOptions result = new VSClientOptions();
            result.setSslTermination(sslTermination);
            return result;
        }
    }
}
