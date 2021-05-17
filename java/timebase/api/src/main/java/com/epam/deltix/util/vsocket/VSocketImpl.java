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

import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.vsocket.transport.Connection;

import java.net.Socket;
import java.io.*;
import java.net.SocketAddress;
import java.util.logging.Level;

/**
 * Date: Mar 5, 2010
 */
public class VSocketImpl implements VSocket {
    // Controls buffer sized. "Default buffer size" lets you set both send and receive buffer size using single argument.
    private static final int        SOCKET_DEFAULT_BUFFER_SIZE = Integer.getInteger("TimeBase.network.socket.bufferSize", 1 << 16);
    private static final int        SOCKET_RECEIVE_BUFFER_SIZE = Integer.getInteger("TimeBase.network.socket.receiveBufferSize", SOCKET_DEFAULT_BUFFER_SIZE);
    private static final int        SOCKET_SEND_BUFFER_SIZE = Integer.getInteger("TimeBase.network.socket.sendBufferSize", SOCKET_DEFAULT_BUFFER_SIZE);

    private static final int        IPTOS_THROUGHPUT = 0x08;

    private Socket                  socket;
    private InputStream             in;
    private BufferedInputStream     bin;

    private OutputStream            out;
    private VSocketOutputStream     vout;
    private VSocketInputStream      vin;
    private String                  remoteAddress;
    private int                     code;
    private final int               socketNumber;


    private void         setUpSocket () {
        try {
            socket.setTcpNoDelay (true);
            socket.setSoTimeout (0);
            socket.setKeepAlive (true);
            socket.setTrafficClass(IPTOS_THROUGHPUT);

            SocketAddress address = socket.getRemoteSocketAddress();
            remoteAddress = address != null ? address.toString() : null;

            // TODO: temp fix for dead-lock in VSockets
            // TODO: Clarify TODO above.

            socket.setReceiveBufferSize(SOCKET_RECEIVE_BUFFER_SIZE);
            socket.setSendBufferSize(SOCKET_SEND_BUFFER_SIZE);
        } catch (IOException x) {
            VSProtocol.LOGGER.log (Level.WARNING, null, x);
        }
    }

    public VSocketImpl(ClientConnection cc, int socketNumber) throws IOException {
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
}
