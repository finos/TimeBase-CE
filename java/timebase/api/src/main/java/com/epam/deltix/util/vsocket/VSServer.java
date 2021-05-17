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

import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.IOUtil;

import java.net.*;
import java.io.*;
import java.util.logging.Level;

/**
 *
 */
public class VSServer extends Thread {
    private final ContextContainer contextContainer;
    private final VSServerFramework     framework;
    private ServerSocket                serverSocket;
    
    public VSServer () throws IOException {
        this (0);
    }

    public VSServer (int port) throws IOException {
        this(port, null, null, null);
    }

    public VSServer(int port, TLSContext ctx) throws IOException {
        this(port, null, ctx, null);
    }

    public VSServer(int port, TransportProperties transportProperties) throws IOException {
        this(port, null, null, transportProperties);
    }

    public VSServer(int port, InetAddress address, TLSContext sslProperties, TransportProperties transportProperties) throws IOException {
        this(VSServerSocketFactory.createServerSocket(port, address), sslProperties, transportProperties);
    }

    public VSServer (ServerSocket serverSocket, TLSContext ctx, TransportProperties transportProperties) {
        super ("VSServer on " + serverSocket);
        // TODO: Probably we should get contextContainer from constructor parameters
        this.contextContainer = new ContextContainer();
        this.contextContainer.setQuickExecutorName("VSServer Executor");
        this.serverSocket = serverSocket;

        contextContainer.getQuickExecutor().reuseInstance();
        this.framework = new VSServerFramework (
                contextContainer.getQuickExecutor(), VSProtocol.LINGER_INTERVAL, VSCompression.AUTO, contextContainer);
        try {
            if (ctx != null)
                this.framework.initSSLSocketFactory(ctx);
        } catch (Exception ex) {
            VSProtocol.LOGGER.log(Level.WARNING, "Failed to init SSL", ex);
        }

        this.framework.initTransport(transportProperties);
    }

    public QuickExecutor        getExecutor () {
        return framework.getExecutor ();
    }

    public int                  getLocalPort () {
        return (serverSocket.getLocalPort ());
    }

    public void                 setConnectionListener (VSConnectionListener lnr) {
        framework.setConnectionListener (lnr);
    }

    public int                  getSoTimeout () throws IOException {
        return serverSocket.getSoTimeout();
    }

    public void                 setSoTimeout (int readTimeout) throws SocketException {
        serverSocket.setSoTimeout(readTimeout);
    }

    @Override
    public void                 run () {
        Socket          s = null;
        for (;;) {
            try {
                s = serverSocket.accept();

                //long t0 = System.nanoTime();
                if (framework.handleHandshake(s))
                    s = null;
                else
                    s.close();
                //long t1 = System.nanoTime();
                //System.out.println("New connection was processed in: " + TimeUnit.NANOSECONDS.toMillis(t1 - t0));

            } catch (IOException iox) {
                IOUtil.close(s);

                if (!serverSocket.isClosed()) {
                    VSProtocol.LOGGER.log(
                            Level.WARNING,
                            "Exception while accepting connections",
                            iox
                    );
                } else {
                    break;
                }
            }
        }

        if (serverSocket != null && !serverSocket.isClosed())
            IOUtil.close (serverSocket);
    }

    public void                 close () {
        IOUtil.close (serverSocket);
        interrupt ();
        contextContainer.getQuickExecutor().shutdownInstance();
        if (framework != null)
            framework.close();
    }
}
