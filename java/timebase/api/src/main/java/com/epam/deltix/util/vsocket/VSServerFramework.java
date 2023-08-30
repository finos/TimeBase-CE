/*
 * Copyright 2023 EPAM Systems, Inc
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
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.aeron.DXAeron;
import com.epam.deltix.util.io.offheap.OffHeap;
import com.epam.deltix.util.lang.DisposableListener;
import com.epam.deltix.util.tomcat.ConnectionHandshakeHandler;
import com.epam.deltix.util.vsocket.transport.Connection;
import com.epam.deltix.util.vsocket.transport.SocketConnectionFactory;

import java.net.*;
import java.util.*;
import java.io.*;
import java.util.logging.Level;

/**
 *
 */
public class VSServerFramework implements ConnectionHandshakeHandler, DisposableListener<VSDispatcher>, Closeable {
    public static volatile VSServerFramework INSTANCE = null;

    static final int         MIN_COMPATIBLE_CLIENT_VERSION = 1014;
    static final int         MAX_COMPATIBLE_CLIENT_VERSION = VSProtocol.VERSION;

    public final static int        MAX_CONNECTIONS                = 100;
    public final static short      MAX_SOCKETS_PER_CONNECTION      = 8;

    private final Map <String, Connector>        dispatchers =
        new HashMap <> ();

    private final QuickExecutor                 executor;
    private final ContextContainer contextContainer;

    private volatile VSConnectionListener       connectionListener;
    private final int                           connectionsLimit;
    private final short                         transportsLimit;
    private final long                          time;
    private int                                 reconnectInterval;
    private final VSCompression                 compression;

    private TLSContext                          tlsContext;

    private TransportType                       transportType = TransportType.SOCKET_TCP;

    public static final Comparator <VSDispatcher> comparator = new Comparator <VSDispatcher>() {

        @Override
        public int              compare (VSDispatcher o1, VSDispatcher o2) {
            return (o1.getCreationDate ().compareTo (o2.getCreationDate ()));
        }
    };

    public VSServerFramework(QuickExecutor executor, int reconnectInterval,
                             VSCompression compression, int connectionsLimit, short socketsPerConnection, ContextContainer contextContainer) {
        this.executor = executor;
        this.reconnectInterval = reconnectInterval;
        this.time = System.currentTimeMillis();
        this.compression = compression;
        this.connectionsLimit = connectionsLimit;
        this.transportsLimit = socketsPerConnection;
        this.contextContainer = contextContainer;
        INSTANCE = this;
    }

    public VSServerFramework(QuickExecutor executor, int reconnectInterval, VSCompression compression, ContextContainer contextContainer) {
        this(executor, reconnectInterval, compression, MAX_CONNECTIONS, MAX_SOCKETS_PER_CONNECTION, contextContainer);
    }

    public QuickExecutor getExecutor () {
        return executor;
    }

    public VSDispatcher []      getDispatchers () {
        VSDispatcher [] ret;

        synchronized (dispatchers) {
            ret = new VSDispatcher[dispatchers.size()];
            int index = 0;
            for (Connector value : dispatchers.values())
                ret[index++] = value.dispatcher;
        }

        Arrays.sort (ret, comparator);
        return (ret);
    }

    public VSDispatcher         getDispatcher(String id) {
        synchronized (dispatchers) {
            Connector connector = dispatchers.get(id);
            return connector != null ? connector.dispatcher : null;
        }
    }

    /*
        Returns current throughput: bytes per second
     */

    public long                 getThroughput() {
        long throughput = 0;
        synchronized (dispatchers) {
            for (Connector value : dispatchers.values())
                throughput += value.dispatcher.getAverageThroughput();
        }

        return throughput;
    }

    public void                 setConnectionListener (VSConnectionListener lnr) {
        connectionListener = lnr;
    }

    public void                 initSSLSocketFactory(TLSContext context) {
        this.tlsContext = context;
    }

    public void                 initTransport(TransportProperties transportProperties) {
        if (transportProperties != null) {
            transportType = transportProperties.transportType;
            if (transportType == TransportType.AERON_IPC) {
                DXAeron.start(transportProperties.transportDir, false);
            } else if (transportType == TransportType.OFFHEAP_IPC) {
                OffHeap.start(transportProperties.transportDir, true);
            }
        }
    }

    public boolean              handleHandshake (Socket s) throws IOException {
        s.setSoTimeout (0);
        s.setTcpNoDelay(true);
        s.setKeepAlive(true);

        return handleHandshake(
            SocketConnectionFactory.createConnection(
                s, new BufferedInputStream(s.getInputStream()), s.getOutputStream())
        );
    }

    /** Handles inbound HTTP connection handshake when TB is running inside Tomcat */
    @Override
    public boolean handleHandshake(Socket s, BufferedInputStream is, OutputStream os) throws IOException {
        s.setSoTimeout (0);
        s.setTcpNoDelay(true);
        s.setKeepAlive(true);

        return handleHandshake(
            SocketConnectionFactory.createConnection(s, is, os)
        );
    }

    /**
     *  Handle the initial transport-level handshake with a client.
     *
     *  @param c    Socket to perform the handshake with.
     *  @return     true if the connection is accepted and socket added to the
     *              set of transport channels. false if socket should be closed.
     *
     *  @throws IOException
     */
    public boolean              handleHandshake (Connection c) throws IOException {
        Level handshakeTimeLogLevel = Level.FINE;
        long handshakeStart = VSProtocol.LOGGER.isLoggable(handshakeTimeLogLevel) ? System.currentTimeMillis() : 0;
        try {
            return handleHandshakeInternal(c);
        } finally {
            if (VSProtocol.LOGGER.isLoggable(handshakeTimeLogLevel)) {
                long handshakeEnd = System.currentTimeMillis();
                VSProtocol.LOGGER.log(handshakeTimeLogLevel, "handleHandshake took " + (handshakeEnd - handshakeStart) + " ms");
            }
        }
    }

    /**
     * See {@link #handleHandshake(Connection)}.
     */
    private boolean              handleHandshakeInternal (Connection c) throws IOException {

        processSSLHandshake(c);

        DataInputStream     dis = new DataInputStream (c.getInputStream());
        DataOutputStream    dout = new DataOutputStream (c.getOutputStream());

        int                 clientVersion = dis.readInt ();

        boolean isCompatible = clientVersion >= MIN_COMPATIBLE_CLIENT_VERSION && clientVersion <= MAX_COMPATIBLE_CLIENT_VERSION;

        dout.writeInt (isCompatible ? clientVersion : VSProtocol.VERSION);
        dout.writeUTF(String.valueOf(VSProtocol.VERSION));
        dout.flush ();

        String              clientId = dis.readUTF ();
        //dout.writeUTF (Version.VERSION_STRING);

        if (!isCompatible) {
            VSProtocol.LOGGER.severe (
                "Connection from " + clientId + " rejected due to incompatible protocol version #" +
                clientVersion + " (accepted: " +
                MIN_COMPATIBLE_CLIENT_VERSION + " .. " +
                MAX_COMPATIBLE_CLIENT_VERSION + ")"
            );

            dout.writeByte (VSProtocol.CONN_RESP_INCOMPATIBLE_CLIENT);
            dout.flush ();
            return (false);
        }

        dout.writeInt(tlsContext == null ? 0 : tlsContext.port);

        if (clientVersion > 1014)
            processTransportHandshake(c);

        boolean             isNew = dis.readBoolean();
        int                 sCode = dis.readInt();
        long                recieved = dis.readLong();

        Connector           connector = process(clientId);
        if (connector == null) {
            dout.writeByte (VSProtocol.CONN_RESP_CONNECTION_REJECTED);
            dout.flush ();
            return (false);
        }

        if (VSProtocol.LOGGER.isLoggable (Level.FINE))
            VSProtocol.LOGGER.fine ("Accepted connection from " + clientId);

        VSocketRecoveryInfo brokenSocketRecoveryInfo = isNew ? null : connector.remove(sCode);
        VSocket broken = null;

        if (brokenSocketRecoveryInfo != null) {
            synchronized (brokenSocketRecoveryInfo) {
                // Prevent other threads against attempting to recover same socket
                if (brokenSocketRecoveryInfo.startRecoveryAttempt()) {
                    broken = brokenSocketRecoveryInfo.getSocket();
                } else {
                    VSProtocol.LOGGER.fine("Recovery attempt failed because another attempt for this thread in progress");
                }
            }
        }

        boolean success = false;
        try {

            String transportTag = sCode + " / " + Integer.toHexString(sCode);
            if (broken != null) {
                VSProtocol.LOGGER.info("Restoring connection (" + transportTag + ") for " + clientId);
                broken.getOutputStream().confirm(recieved);
            } else {
                if (!isNew) {
                    VSProtocol.LOGGER.warning("Connection restore failed for transport (" + transportTag + ") for " + clientId + " because server side transport is not found");
                }
            }

            dout.writeByte(VSProtocol.CONN_RESP_OK);
            dout.writeLong(time);
            dout.writeInt(reconnectInterval);
            dout.writeUTF(compression.toString());

            // writing -1 means socket wasn't found
            dout.writeLong(broken != null ? broken.getInputStream().getBytesRead() : (isNew ? 0 : -1));
            dout.flush();

            VSocket socket = broken != null ? c.create(broken) : c.create(sCode);

            if (!connector.addTransportChannel(socket)) {
                VSProtocol.LOGGER.info("Connection(" + transportTag + ") rejected for " + clientId);
                return (false);
            }

            if (broken != null)
                VSProtocol.LOGGER.info("Connection(" + transportTag + ") restored for " + clientId);

            success = true;
            return (true);
        } finally {
            if (broken != null) {
                // assert brokenSocketRecoveryInfo != null;
                synchronized (brokenSocketRecoveryInfo) {
                    brokenSocketRecoveryInfo.stopRecoveryAttempt();
                    if (success) {
                        brokenSocketRecoveryInfo.markRecoverySucceeded();
                    }
                    brokenSocketRecoveryInfo.notifyAll();
                }
            }
        }
    }

    private Connector                process(String clientId) {
        Connector connector;

        synchronized (dispatchers) {
            connector = dispatchers.get(clientId);

            if (connector == null && dispatchers.size() >= connectionsLimit) {
                VSProtocol.LOGGER.severe (
                        "Connection from " + clientId + " rejected due to connections limit = (" + connectionsLimit + ")"
                );
                return null;
            }

            if (connector == null) {
                VSDispatcher dispatcher = new VSDispatcher (clientId, false, contextContainer);
                dispatcher.setConnectionListener(connectionListener);
                dispatcher.setLingerInterval(reconnectInterval);
                dispatcher.addDisposableListener(this);
                dispatchers.put (clientId, (connector = new Connector(dispatcher, transportsLimit)));
            }
        }

        return connector;
    }

    private void                    processSSLHandshake(Connection c) throws IOException {
        boolean enableSSL = (tlsContext != null);
        boolean sslForLoopback = (tlsContext != null) && !tlsContext.preserveLoopback;

        BufferedInputStream     is = c.getInputStream();
        OutputStream            os = c.getOutputStream();

        //initial byte (0 for VS protocol)
        int b = is.read();
        assert b == 0;

        int clientHeader = is.read();
        if (clientHeader == VSProtocol.SSL_HEADER && !enableSSL) {
            os.write(VSProtocol.CONN_RESP_SSL_NOT_SUPPORTED);
            throw new IOException("Client wants SSL but server have not prepared for handshake.");
        }
        os.write(VSProtocol.CONN_RESP_OK);

        //server choice
        boolean sslConnection = false;
        if (enableSSL && sslForLoopback)
            sslConnection = true;
        else if (enableSSL && !sslForLoopback && !c.isLoopback())
            sslConnection = true;
        else if (enableSSL && !sslForLoopback && c.isLoopback() && clientHeader == VSProtocol.SSL_HEADER)
            sslConnection = true;

        //write server decision (SSL or NON-SSL)
        if (sslConnection) {
            os.write(VSProtocol.SSL_HEADER);
            c.upgradeToSSL(tlsContext.context.getSocketFactory());
            VSProtocol.LOGGER.fine("Socket upgraded to SSL socket! Now connection is secured.");
        } else {
            os.write(VSProtocol.HEADER);
        }
    }

    private void                    processTransportHandshake(Connection c) throws IOException {
        DataOutputStream dout = new DataOutputStream (c.getOutputStream());

        TransportType type = (!c.isLoopback() || transportType == null) ? TransportType.SOCKET_TCP : transportType;
        dout.writeInt(type.ordinal());

        c.setTransportType(type);
        if (type == TransportType.AERON_IPC)
            dout.writeUTF(DXAeron.getAeronDir());
        else if (type == TransportType.OFFHEAP_IPC)
            dout.writeUTF(OffHeap.getOffHeapDir());
    }

    public VSCompression            getCompression() {
        return compression;
    }

    @Override
    public void                     disposed(VSDispatcher resource) {
        synchronized (dispatchers) {
            Connector c = dispatchers.remove(resource.getClientId());
            if (c != null)
                c.close();
        }
    }

    @Override
    public void close() {
        if (transportType == TransportType.AERON_IPC)
            DXAeron.shutdown();
    }

    static class Connector extends ConnectionStateListener implements Closeable {
        // May contain null values. Null value indicates that transport is still considered active (not stopped).
        private final IntegerToObjectHashMap<VSocketRecoveryInfo>   stopped =
                new IntegerToObjectHashMap<>();

        private final VSDispatcher dispatcher;
        private final int           limit; // limit of transport channels

        Connector(VSDispatcher d, int limit) {
            this.dispatcher = d;
            this.limit = limit;
            dispatcher.setStateListener(this);
        }

        boolean addTransportChannel(VSocket socket) throws IOException {

            synchronized (stopped) {

                // not accept new channels, only that can be restored
                if (!stopped.containsKey(socket.getCode()) && stopped.size() >= limit)
                    return false;

                // Note: we may override previous state of socket here (in case of recovery)
                stopped.put(socket.getCode(), ACTIVE);
                stopped.notifyAll();
            }

            dispatcher.addTransportChannel(socket);
            return true;
        }

        @Override
        boolean onTransportStopped(VSocketRecoveryInfo recoveryInfo) {
            VSocket socket = recoveryInfo.getSocket();

            int code = socket.getCode();
            synchronized (stopped) {
                VSocketRecoveryInfo prevValue = stopped.get(code, null);
                if (prevValue != ACTIVE) {
                    String clientId = dispatcher.getClientId();
                    if (prevValue == null) {
                        VSProtocol.LOGGER.severe("Transport for " + clientId + " has stopped. It can't be recovered because the recovery state is invalid.");
                    } else {
                        VSProtocol.LOGGER.warning("Transport for " + clientId + " has stopped. It can't be recovered because a recovery attempt is already initiated.");
                    }
                    return true;
                }
                stopped.put(code, recoveryInfo);
                stopped.notifyAll();
                return false;
            }
        }

        @Override
        boolean onTransportBroken(VSocketRecoveryInfo recoveryInfo) {
            try {
                synchronized (recoveryInfo) {
                    while (recoveryInfo.isRecoveryAttemptInProgress()) {
                        recoveryInfo.wait();
                    }
                    if (recoveryInfo.isRecoverySucceeded()) {
                        return false;
                    }
                    recoveryInfo.markRecoveryFailed();

                    VSocket socket = recoveryInfo.getSocket();
                    int code = socket.getCode();
                    synchronized (stopped) {
                        VSocketRecoveryInfo currentValue = stopped.get(code, null);
                        if (currentValue == recoveryInfo) {
                            stopped.remove(code);
                            stopped.notifyAll();
                            recoveryInfo.notifyAll();
                            return true;
                        }
                    }

                    recoveryInfo.notifyAll();
                    return false;
                }
            } catch (InterruptedException e) {
                return true;
            }
        }

        @Override
        public void         close() {
            dispatcher.setStateListener(null);

            synchronized (stopped) {
                stopped.clear();
                stopped.notifyAll();
            }
        }

        /**
         * Attempts to get transport for a recovery attempt.
         */
        public VSocketRecoveryInfo remove(int code) {
            try {
                synchronized (stopped) {
                    VSocketRecoveryInfo currentValue;
                    while (true) {
                        currentValue = stopped.get(code, null);
                        // request for restoring connection may came faster that socket is stopped
                        if (currentValue == ACTIVE) {
                            stopped.wait();
                        } else {
                            break;
                        }
                    }

                    // assert currentValue != ACTIVE

                    if (currentValue == null) {
                        // This transport never existed OR permanently removed
                        return null;
                    }

                    return currentValue;
                }
            } catch (InterruptedException e) {
                return null;
            }
        }

        @Override
        void onDisconnected() {
            synchronized (stopped) {
                stopped.clear();
                stopped.notifyAll();
            }
        }

        @Override
        void onReconnected() {
        }
    }

    // Special marker values

    // This value indicates that the transport for corresponding code is active (not stopped yet)
    private static final FakeRecoveryInfo ACTIVE = new FakeRecoveryInfo("ACTIVE");

    /**
     * Special class to represent special values in {@link Connector#stopped} map.
     */
    private static class FakeRecoveryInfo extends VSocketRecoveryInfo {
        private final String label;

        FakeRecoveryInfo(String label) {
            super(null, Long.MIN_VALUE);
            this.label = label;
        }

        @Override
        public String toString() {
            return "FakeVSocket{" +
                    "" + label + '\'' +
                    '}';
        }
    }
}