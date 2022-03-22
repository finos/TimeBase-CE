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
package com.epam.deltix.qsrv.hf.tickdb.http.rest;

import com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBWrapper;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.util.codec.Base64DecoderEx;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.security.SecurityController;
import com.epam.deltix.util.tomcat.ConnectionHandshakeHandler;
import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.vsocket.TLSContext;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/**
 *
 */
public class RESTHandshakeHandler implements ConnectionHandshakeHandler, Closeable {

    private static final int        KEEP_ALIVE_INTERVAL = 5000;

    private SecurityController      securityController;
    private KeepAlive               keepAlive = null;
    private DXTickDB                tickdb;
    private final Map<String, DXTickDB> userNameToDb = new HashMap<>();
    private final ContextContainer contextContainer;
    private final TLSContext        tlsContext;

    public RESTHandshakeHandler(DXTickDB tickdb,
                                SecurityController securityController,
                                ContextContainer contextContainer,
                                TLSContext tlsContext)
    {
        this.tickdb = tickdb;
        this.contextContainer = contextContainer;
        this.contextContainer.getQuickExecutor().reuseInstance();
        this.securityController = securityController;
        this.tlsContext = tlsContext;
    }

    public boolean handleHandshake(Socket socket, BufferedInputStream bis, OutputStream os) throws IOException {

        synchronized (this) {
            if (keepAlive == null) {
                keepAlive = new KeepAlive(contextContainer.getQuickExecutor());
                keepAlive.submit();
            }
        }

        setUpSocket(socket);

        final DataInputStream dis = new DataInputStream(bis);
        final DataOutputStream dos = new DataOutputStream(os);

        final int init = dis.read();
        assert HTTPProtocol.PROTOCOL_INIT == init;

        if (!handshakeVersion(dis, dos))
            return false;

        DXTickDB db = readCredentialsAndAuthenticate(dis, dos);
        dos.writeInt(HTTPProtocol.RESP_OK);

        final RestHandler handler;
        final int request = dis.readByte();

        switch (request) {
            case HTTPProtocol.REQ_UPLOAD_DATA:
                handler = new UploadHandler(db, socket, bis, os, contextContainer);
                break;

            case HTTPProtocol.REQ_CREATE_SESSION:
                handler = new SessionHandler(db, socket, bis, os, contextContainer);
                break;

            default:
                HTTPProtocol.LOGGER.severe("Unrecognized request code: " + request);
                return false;
        }

        keepAlive.handlers.add(handler);
        handler.submit();

        return true;
    }

    private void                    setUpSocket(Socket socket) {
        try {
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(0);
            socket.setKeepAlive(true);
        } catch (IOException x) {
            HTTPProtocol.LOGGER.log(Level.WARNING, null, x);
        }
    }

    private boolean                  handshakeVersion(DataInputStream dis, DataOutputStream dos) throws IOException {
        dos.writeShort(HTTPProtocol.VERSION);
        final short version = dis.readShort();
        if (version < HTTPProtocol.MIN_CLIENT_VERSION) {
            HTTPProtocol.LOGGER.severe(
                String.format("Incompatible REST-TB protocol version %d. Minimal expected version is %d.", version, HTTPProtocol.MIN_CLIENT_VERSION));
            return false;
        }

        return true;
    }

    private DXTickDB                readCredentialsAndAuthenticate(DataInputStream dis, DataOutputStream dos) throws IOException {
        String user = null;
        String password = null;

        final boolean readPrincipal = dis.readBoolean();
        if (readPrincipal) {
            user = dis.readUTF();
            password = new String(Base64DecoderEx.decodeBuffer(dis.readUTF()), StandardCharsets.UTF_8);
        }

        DXTickDB db = tickdb;

        if (securityController != null) {
            try {
                Principal principal = securityController.authenticate(user, password);
                if (principal != null) {
                    synchronized (userNameToDb) {
                        db = userNameToDb.get(principal.getName());
                        if (db == null)
                            userNameToDb.put(principal.getName(), (db = new TickDBWrapper(tickdb, securityController, principal)));
                    }
                } else
                    throw new AccessControlException("User is not specified.");
            } catch (Throwable t) {
                HTTPProtocol.LOGGER.log(Level.SEVERE, "Authentication error: ", t);
                responseError(dos, t);
                throw t;
            }
        }

        return db;
    }

    private void                    responseError(DataOutputStream dos, Throwable t) throws IOException {
        dos.writeInt(HTTPProtocol.RESP_ERROR);

        dos.writeUTF(t.getClass().getName());
        String msg = t.getMessage();
        dos.writeUTF(msg == null ? "" : msg);
    }

    public void                     close() {
        if (keepAlive != null)
            keepAlive.close();
        this.contextContainer.getQuickExecutor().shutdownInstance();
    }

    private class KeepAlive extends QuickExecutor.QuickTask implements Disposable {
        List<RestHandler>           handlers = new CopyOnWriteArrayList<>();
        private volatile boolean    closed = false;

        public KeepAlive(QuickExecutor executor) {
            super(executor);
        }

        @Override
        public void run() throws InterruptedException {
            List<RestHandler> toRemove = new ArrayList<>();

            while (!closed) {
                for (int i = 0; i < handlers.size(); ++i) {
                    RestHandler handler = handlers.get(i);
                    try {
                        handler.sendKeepAlive();
                    } catch (IOException e) {
                        toRemove.add(handler);
                    }
                }

                handlers.removeAll(toRemove);
                toRemove.clear();
                Thread.sleep(KEEP_ALIVE_INTERVAL);
            }
            handlers.clear();
        }

        public void close() {
            closed = true;
        }
    }

}
