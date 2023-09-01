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
package com.epam.deltix.util.vsocket.transport;

import com.epam.deltix.util.vsocket.TransportType;
import com.epam.deltix.util.vsocket.VSProtocol;
import com.epam.deltix.util.vsocket.VSocket;
import com.epam.deltix.util.vsocket.VSocketFactory;
import com.epam.deltix.util.vsocket.VSocketImpl;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.logging.Level;

/**
 *
 */
public class DatagramConnection implements Connection {
    public DatagramSocket       socket;
    private BufferedInputStream in;
    private OutputStream        out;
    int                         remotePort = -1;

    public DatagramConnection(DatagramSocket socket, InetAddress address) throws UnknownHostException {
        this.socket = socket;
        this.in = new BufferedInputStream(new DatagramInputStream(this));
        this.out = new DatagramOutputStream(this, address);
        setUpSocket();
    }

//    public DatagramConnection(DatagramSocket socket, InetSocketAddress address) throws UnknownHostException {
//        this.socket = socket;
//        this.in = new BufferedInputStream(new DatagramInputStream(this));
//        this.out = new DatagramOutputStream(this, address.getAddress());
//        this.remotePort = address.getPort();
//        setUpSocket();
//    }

    private void         setUpSocket () {
        try {
            socket.setSoTimeout (0);

            //socket.setReceiveBufferSize(1 << 14);
            //socket.setSendBufferSize(1 << 14);
        } catch (SocketException e) {
            VSProtocol.LOGGER.log (Level.WARNING, null, e);
        }
    }

    @Override
    public OutputStream         getOutputStream() {
        return out;
    }

    @Override
    public BufferedInputStream  getInputStream() {
        return in;
    }

    @Override
    public VSocket              create(int code) {
        return new VSocketImpl(this, code, VSocketFactory.nextSocketNumber());
    }

    @Override
    public InetAddress          getRemoteAddress() {
        return null;
    }

    @Override
    public void                 close() {
        socket.close();
    }

    @Override
    public VSocket              create(VSocket stopped) throws IOException {
        VSocket socket = create(stopped.getCode());
        stopped.getOutputStream().writeTo(socket.getOutputStream());
        return socket;
    }

    @Override
    public boolean              isLoopback() {
        return socket.getInetAddress().isLoopbackAddress();
    }

    @Override
    public void setTransportType(TransportType transportType) {
        /* not implemented for this class */
    }

    @Override
    public void                 upgradeToSSL(SSLSocketFactory sslSocketFactory) throws IOException {
        /* not implemented for this class */
    }
}