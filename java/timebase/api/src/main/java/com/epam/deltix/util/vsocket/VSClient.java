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

import com.google.common.annotations.VisibleForTesting;
import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.aeron.DXAeron;
import com.epam.deltix.util.io.offheap.OffHeap;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.util.lang.DisposableListener;
import com.epam.deltix.util.time.GlobalTimer;
import com.epam.deltix.util.time.TimeKeeper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;

/**
 *
 */
public class VSClient extends ConnectionStateListener implements Disposable, DisposableListener<VSDispatcher> {
    public static final int             MIN_COMP_SERVER_VERSION = VSProtocol.VERSION;
    public static final int             MAX_COMP_SERVER_VERSION = VSProtocol.VERSION;

    //private static final int MAX_TRANSPORT_RECONNECT_ATTEMPTS = Integer.getInteger("TimeBase.network.VSClient.maxTransportReconnectAttempts", 5);
    private static final int TRANSPORT_RECONNECT_ATTEMPT_INTERVAL = Integer.getInteger("TimeBase.network.VSClient.transportReconnectAttemptInterval", 1000);

    private static final int RE_ATTEMPT_EXTRA_DELAY = 10; // Extra delay to avoid situation when we re-schedule task due to timer jitter

    private String                      host;
    private int                         port;
    private int                         numTransportChannels = 3;

    private volatile VSDispatcher       dispatcher;
    // Protects "dispatcher" field and interactions with quick executor
    private final Object dispatcherLock = new Object();

    private final String                clientId;
    private long                        serverTime = -1;    

    private volatile DisconnectEventListener     listener;
    private int                         reconnectInterval;
    private VSCompression               serverCompression;

    private int                         soTimeout = Integer.getInteger("TimeBase.network.VSClient.soTimeout", 5000);
    private int                         timeout = Integer.getInteger("TimeBase.network.VSClient.timeout", 5000);

    private boolean                     enableSSL = false;
    private int                         sslPort = 0;
    private SSLContext                  sslContext;

    private boolean                     aeronEnabled = false;

    private final ContextContainer      contextContainer;

    private volatile boolean closed = false;

    // Elements should be sorted (when possible) by time of last reconnection attempt however there is no strict enforcement for this.
    // New broken sockets should be added to the head of the queue
    private final ConcurrentLinkedDeque<VSocketRecoveryInfo> broken = new ConcurrentLinkedDeque<>();

    private final QuickExecutor.QuickTask reconnector;

    private QuickExecutor.QuickTask createReconnectorTask(final QuickExecutor quickExecutor) {
        return new QuickExecutor.QuickTask(quickExecutor) {
            @Override
            public void         run() {
                for (;;) {
                    long currentTime = TimeKeeper.currentTime;

                    VSocketRecoveryInfo socketRecovery = broken.peek();

                    if (dispatcher == null || socketRecovery == null)
                        break;

                    long lastReconnectAttemptTs = socketRecovery.getLastReconnectAttemptTs();
                    if (lastReconnectAttemptTs > currentTime - TRANSPORT_RECONNECT_ATTEMPT_INTERVAL) {
                        // It's too early to recover this socket
                        scheduleReconnectAttempt(lastReconnectAttemptTs + TRANSPORT_RECONNECT_ATTEMPT_INTERVAL + RE_ATTEMPT_EXTRA_DELAY);
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
                        int attemptNumber = socketRecovery.addReconnectAttempt(currentTime);


                        boolean success = false;
                        boolean transportLost = false;
                        try {
                            VSocket vSocket = openTransport(socket);
                            if (vSocket != null) {
                                success = true;
                                dispatcher.addTransportChannel(vSocket);
                                synchronized (socketRecovery) {
                                    if (!socketRecovery.isRecoveryEnded()) {
                                        socketRecovery.markRecoverySucceeded();
                                        socketRecovery.notifyAll();
                                    } else {
                                        VSProtocol.LOGGER.log(Level.WARNING, "Reconnect succeeded but recovery process is already cancelled");
                                    }
                                }
                                VSProtocol.LOGGER.log(Level.INFO, "Reconnect success, connection " + socket.getSocketIdStr() + ", address " + socket.getRemoteAddress() + " after " + attemptNumber + " attempts");
                            } else {
                                VSProtocol.LOGGER.log(Level.INFO, "Reconnect failed (no error), connection " + socket.getSocketIdStr() + ", address " + socket.getRemoteAddress() + ", attempt " + attemptNumber);
                            }
                        } catch (IOException e) {
                            VSProtocol.LOGGER.log(Level.INFO, "Reconnect failed (" + e.getMessage() + "), connection " + socket.getSocketIdStr() + ", address " + socket.getRemoteAddress() + ", attempt " + attemptNumber);
                        } catch (TransportRecoveryFailre transportRecoveryFailre) {
                            transportLost = true;
                        }

                        if (!success) {
                            if (currentTime > socketRecovery.getDisconnectTs() + reconnectInterval) {
                                // At this time socket is discarded on the server side so we should give up now
                                VSProtocol.LOGGER.log(Level.WARNING, "Transport " + socket.getSocketIdStr() + " was not recovered after " + attemptNumber + " attempts (timeout reached)");
                                transportLost = true;
                            }
                            if (transportLost) {
                                // We failed to recover the connection so we have to disconnect entire transport because we might loss some data
                                synchronized (socketRecovery) {
                                    socketRecovery.stopRecoveryAttempt();
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
        GlobalTimer.INSTANCE.schedule(new TimerTask() {
            @Override
            public void run() {
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

    public VSClient(String host, int port, String ownerID, boolean enableSSL, ContextContainer contextContainer) throws IOException {
        this.host = host;
        this.port = port;
        this.enableSSL = enableSSL;
        this.contextContainer = contextContainer;
        this.reconnector = createReconnectorTask(contextContainer.getQuickExecutor());

        if (ownerID == null)
            this.clientId = new GUID().toStringWithPrefix (InetAddress.getLocalHost().getHostAddress() + ":");
        else
            this.clientId = new GUID().toStringWithPrefix(InetAddress.getLocalHost().getHostAddress() + ":" + ownerID + ":");

        /*
        if (TRANSPORT_RECONNECT_ATTEMPT_INTERVAL < soTimeout) {
            VSProtocol.LOGGER.warning("Reconnect interval (" + TRANSPORT_RECONNECT_ATTEMPT_INTERVAL + ") should not be less than socket open timeout (" + soTimeout + ")");
        }
        */
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

    public boolean                  isConnected() {
        return dispatcher != null && dispatcher.hasAvailableTransport();
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

    private Socket              processSSLHandshake(Socket socket) throws IOException {
        InputStream     is = socket.getInputStream();
        OutputStream    os = socket.getOutputStream();

        os.write(0); //first byte of VS protocol
        os.write(VSProtocol.getHeader(enableSSL));
        os.flush();

        int serverResponse = is.read();
        if (serverResponse == VSProtocol.CONN_RESP_SSL_NOT_SUPPORTED)
            throw new IOException("Server not supported SSL.");

        int serverHeader = is.read();
        if (serverHeader == VSProtocol.SSL_HEADER) {
            socket = sslContext.getSocketFactory().createSocket(
                socket, socket.getInetAddress().getHostAddress(), socket.getPort(), false);
            ((SSLSocket) socket).setUseClientMode(true);
            ((SSLSocket) socket).startHandshake();
            enableSSL = true;
            VSProtocol.LOGGER.info("Socket upgraded to SSL socket! Now connection is secured.");
        } else {
            if (enableSSL)
                VSProtocol.LOGGER.info("Connection isn't secured.");
            enableSSL = false;
        }

        return socket;
    }

    @SuppressFBWarnings(value = "UNENCRYPTED_SOCKET", justification = "Timebase ports should be protected from public access by SSL-terminating NLB")
    private VSocket                         openTransport (VSocket stopped) throws IOException, TransportRecoveryFailre {
        Socket              s = new Socket();
        boolean             ok = false;

        try {
            s.setSoTimeout(soTimeout);
            s.setTcpNoDelay(true);
            s.connect(new InetSocketAddress(host, port), timeout);

            s = processSSLHandshake(s);

            ClientConnection cc = new ClientConnection(s);

            DataOutputStream    dos = new DataOutputStream (cc.getOutputStream());
            DataInputStream     dis = new DataInputStream (cc.getBufferedInputStream());

            //check version compatibility
            dos.writeInt (VSProtocol.VERSION);
            dos.writeUTF(clientId);
            dos.flush();

            int spv = dis.readInt ();

            String sid = dis.readUTF ();
            if (spv < MIN_COMP_SERVER_VERSION || spv > MAX_COMP_SERVER_VERSION)
                throw new IncompatibleClientException (sid, spv);

            sslPort = dis.readInt();
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
        Socket              s = new Socket();
        boolean             ok = false;
        TransportType transportType;
        ClientConnection cc;

        try {
            s.setSoTimeout(soTimeout);
            s.setTcpNoDelay(true);
            s.connect(new InetSocketAddress(host, port), timeout);

            s = processSSLHandshake(s);
            cc = new ClientConnection(s);

            DataOutputStream    dos = new DataOutputStream (cc.getOutputStream());
            DataInputStream     dis = new DataInputStream (cc.getBufferedInputStream());

            //check version compatibility
            dos.writeInt (VSProtocol.VERSION);
            dos.writeUTF(clientId);
            dos.flush ();
            int spv = dis.readInt ();
            String sid = dis.readUTF ();
            if (spv < MIN_COMP_SERVER_VERSION || spv > MAX_COMP_SERVER_VERSION)
                throw new IncompatibleClientException (sid, spv);

            sslPort = dis.readInt();
            transportType = processTransportHandshake(dis);

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
            String aeronDir = dis.readUTF();
            if (!aeronEnabled) {
                DXAeron.start(aeronDir, false);
                aeronEnabled = true;
            }
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
            throw new InterruptedIOException ();
        }

        return (vsc);
    }

    @Override
    public void                     close () {
        close(true);
    }

    private void close(boolean waitForChannelsToFinish) {
        synchronized (dispatcherLock) {
            closed = true;

            if (aeronEnabled)
                DXAeron.shutdown();

            VSDispatcher d = dispatcher;

            if (d != null) {
                d.setStateListener(null);
                d.removeDisposableListener(this);
                d.close(waitForChannelsToFinish);

                contextContainer.getQuickExecutor().shutdownInstance();
            }

            dispatcher = null;
        }
    }

    public void                     setDisconnectedListener(DisconnectEventListener listener) {
        this.listener = listener;
    }

    @Override
    boolean onTransportStopped(VSocketRecoveryInfo recoveryInfo) {
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
    boolean onTransportBroken(VSocketRecoveryInfo recoveryInfo) {
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
            return true;
        }
    }

    @Override
    void                            onReconnected() {
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

            if (aeronEnabled)
                DXAeron.shutdown();

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
}
