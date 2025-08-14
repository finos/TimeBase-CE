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

import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.vsocket.transport.Connection;

import java.net.Socket;
import java.io.*;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.logging.Level;

/**
 * Date: Mar 5, 2010
 */
public class VSocketImpl implements VSocket {
    // Buffer size should be at least big enough to fit maximum packet size (VSProtocol.MAX_SIZE).
    // Controls buffer sized. "Default buffer size" lets you set both send and receive buffer size using single argument.
    public static final int         SOCKET_DEFAULT_BUFFER_SIZE = Integer.getInteger("TimeBase.network.socket.bufferSize", 1 << 16);
    public static final int         SOCKET_RECEIVE_BUFFER_SIZE = Integer.getInteger("TimeBase.network.socket.receiveBufferSize", SOCKET_DEFAULT_BUFFER_SIZE);
    public static final int         SOCKET_SEND_BUFFER_SIZE = Integer.getInteger("TimeBase.network.socket.sendBufferSize", SOCKET_DEFAULT_BUFFER_SIZE);

    //@ApiStatus.Experimental // Temporary option for testing performance effect of using buffered reader of different size
    // 8kb size matches to the previous value. However it's very likely that we need 64k or 128k buffer size to match the maximum "logical packet" size (VSProtocol.MAXSIZE).
    // TODO: Consider increasing default value to 64kb.
    public static final int INPUT_STREAM_BUFFER_SIZE = Integer.getInteger("TimeBase.network.streamBufferSize", 8 * 1024);

    public static final boolean PRINT_VSOCKET_SETTINGS = Boolean.getBoolean("TimeBase.network.printSettings");

    static {
        if (PRINT_VSOCKET_SETTINGS) {
            System.out.println("SOCKET_RECEIVE_BUFFER_SIZE: " + SOCKET_RECEIVE_BUFFER_SIZE);
            System.out.println("SOCKET_SEND_BUFFER_SIZE: " + SOCKET_SEND_BUFFER_SIZE);
            System.out.println("INPUT_STREAM_BUFFER_SIZE: " + INPUT_STREAM_BUFFER_SIZE);
        }
    }

    private static final int        IPTOS_THROUGHPUT = 0x08;
    private static final int        DEFAULT_TRAFFIC_CLASS = IPTOS_THROUGHPUT;
    /** Value for {@link Socket#setTrafficClass(int)} */
    public static final int         SOCKET_TRAFFIC_CLASS = validateTrafficClassValue(Integer.getInteger("TimeBase.network.socket.tos", DEFAULT_TRAFFIC_CLASS));

    private final Socket              socket;
    private final InputStream         in;
    private final BufferedInputStream bin;

    private final OutputStream        out;
    private final VSocketOutputStream vout;
    private final VSocketInputStream  vin;
    private String                  remoteAddress;
    private int                     code;
    private final int               socketNumber;


    /**
     * Configures socket.
     *
     * <p>Current implementation of {@link VSClient} executes this on a connected socket.
     * While this is allowed to change socket buffer sizes after the connection is established,
     * it may have different effects on different platforms.</p>
     *
     * <p>Most importantly, TCP Window size may be limited by 64k if receive buffer size
     * is set after the connection is established.</p>
     */
    private void         setUpSocket () {
        try {
            socket.setTcpNoDelay (true);
            socket.setSoTimeout (0);
            socket.setKeepAlive (true);

            // This is likely to have no effect as corresponding TOS field is deprecated.
            // See https://en.wikipedia.org/wiki/Type_of_service
            // and https://en.wikipedia.org/wiki/Differentiated_services
            socket.setTrafficClass(SOCKET_TRAFFIC_CLASS);

            SocketAddress address = socket.getRemoteSocketAddress();
            remoteAddress = address != null ? address.toString() : null;

            // We do not change socket buffer sizes here because
            // we expect that they are already set using configureBufferSizes(...) method or manually.
        } catch (IOException x) {
            VSProtocol.LOGGER.log (Level.WARNING, null, x);
        }
    }

    /**
     * Configures buffer sizes for a socket.
     *
     * <p>It's important to configure decent buffer sizes.
     * It should be at least big enough to fit maximum packet size ({@link VSProtocol#MAXSIZE}).
     * Otherwise, there is a possible situation when TimeBase client and server may run into a deadlock,
     * when transport thread gets stuck in blocking write into socket.</p>
     *
     * <p>Please note that if socket is already connected, then OS may ignore these values.</p>
     *
     * Also please note that in case of server-side socket, the "receive" buffer size should be set on
     * {@link java.net.ServerSocket} using {@link java.net.ServerSocket#setReceiveBufferSize(int)} method.
     * On the client socket, the "receive" buffer size should be set before the connection is established.
     * Otherwise, TCP window size will be limited by 64k.
     */
    public static void configureBufferSizes(Socket socket) throws SocketException {
        socket.setReceiveBufferSize(SOCKET_RECEIVE_BUFFER_SIZE);
        socket.setSendBufferSize(SOCKET_SEND_BUFFER_SIZE);
    }

    public VSocketImpl(ClientConnection cc, int socketNumber) {
        this.socket = cc.getSocket();
        this.in = cc.getInputStream ();
        this.bin = cc.getBufferedInputStream();
        this.out = cc.getOutputStream();
        this.code = this.socket.hashCode();
        this.socketNumber = socketNumber;
        String socketIdStr = getSocketIdStr();
        this.vout = new VSocketOutputStream(out, socketIdStr);
        this.vin = new VSocketInputStream(bin, socketIdStr);
        setUpSocket ();
    }

    public VSocketImpl(Socket s, BufferedInputStream in, OutputStream out, int code, int socketNumber) {
        this.socket = s;
        this.in = this.bin = in;
        this.out = out;
        this.code = code;
        this.socketNumber = socketNumber;
        String socketIdStr = getSocketIdStr();
        this.vout = new VSocketOutputStream(out, socketIdStr);
        this.vin = new VSocketInputStream(bin, socketIdStr);
        setUpSocket ();
    }

    public VSocketImpl(Connection c, int code, int socketNumber) {
        this.in = this.bin = c.getInputStream();
        this.out = c.getOutputStream();
        this.socketNumber = socketNumber;
        this.code = code;
        String socketIdStr = getSocketIdStr();
        this.vout = new VSocketOutputStream(out, socketIdStr);
        this.vin = new VSocketInputStream(bin, socketIdStr);
        this.socket = null;
        //setUpSocket ();
    }

    @Override
    public int                          getCode() {
        return code;
    }

    @Override
    public void                         setCode(int code) {
        this.code = code;
    }

    @Override
    public VSocketInputStream           getInputStream() {
        return vin;
    }

    @Override
    public VSocketOutputStream          getOutputStream() {
        return vout;
    }

    @Override
    public String                       getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public void                         close() {
        IOUtil.close (socket);
        Util.close (in);
    	Util.close (out);
    }

    @Override
    public String toString() {
        return getClass().getName() + getSocketIdStr();
    }

    @Override
    public int getSocketNumber() {
        return socketNumber;
    }

    private static int validateTrafficClassValue(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("Invalid value for TimeBase.network.socket.tos: " + value);
        }
        return value;
    }
}