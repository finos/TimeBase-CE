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
package com.epam.deltix.util.vsocket.transport;

import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.vsocket.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;

/**
 *
 */
public class SocketConnection implements Connection {
    protected Socket socket;
    private BufferedInputStream in;
    private OutputStream out;

    private TransportType transportType = TransportType.SOCKET_TCP;

    public SocketConnection(Socket socket, BufferedInputStream in, OutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    @Override
    public OutputStream             getOutputStream() {
        return out;
    }

    @Override
    public BufferedInputStream      getInputStream() {
        return in;
    }

    @Override
    public VSocket                  create(int code) throws IOException {
        return create(code, transportType);
    }

    @Override
    public VSocket                  create(VSocket stopped) throws IOException {
        VSocket s = create(stopped.getCode());
        if (VSProtocol.LOGGER.isLoggable(Level.INFO)) {
            VSProtocol.LOGGER.log(Level.INFO, "Restore socket " + s + " from: " + stopped);
        }
        stopped.getOutputStream().writeTo(s.getOutputStream());
        return s;
    }

    private VSocket                 create(int code, TransportType type) throws IOException {
        int socketNumber = VSocketFactory.nextSocketNumber();
        if (type == TransportType.AERON_IPC)
            return new AeronIpcSocket(socket, code, true, socketNumber);
        else if (type == TransportType.OFFHEAP_IPC)
            return new OffHeapIpcSocket(socket, code, true, socketNumber);
        else
            return new VSocketImpl(socket, in, out, code, socketNumber);
    }

    @Override
    public InetAddress              getRemoteAddress() {
        return socket.getInetAddress();
    }

    @Override
    public void                     close() {
        IOUtil.close(socket);
    }

    @Override
    public boolean                  isLoopback() {
        return socket.getInetAddress().isLoopbackAddress();
    }

    @Override
    public void                     setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }

    @Override
    public void                     upgradeToSSL(SSLSocketFactory sslSocketFactory) throws IOException {
        //upgrade socket
        if (sslSocketFactory == null)
            throw new IOException("SSLSocketFactory isn't initialized.");

        socket = sslSocketFactory.createSocket(
                    socket,
                    socket.getInetAddress().getHostAddress(),
                    socket.getPort(), false);

        //do handshake
        ((SSLSocket) socket).setUseClientMode(false);
        ((SSLSocket) socket).startHandshake();

        //upgrade streams
        in = new BufferedInputStream(socket.getInputStream());
        out = socket.getOutputStream();
    }
}
