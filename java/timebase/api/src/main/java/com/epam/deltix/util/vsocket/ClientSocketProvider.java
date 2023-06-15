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

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 
 */
public class ClientSocketProvider {

    public static Socket        open(SocketFactory factory, String host, int port, int connectTimeout, int timeout) throws IOException {
        //create and connect
        Socket socket = factory.createSocket();

        socket.setSoTimeout(timeout);
        socket.connect(new InetSocketAddress(host, port), connectTimeout);

        if (socket instanceof SSLSocket) {
            //do ssl handshake
            ((SSLSocket) socket).startHandshake();
            VSProtocol.LOGGER.fine("VSClient connected to SSL server " + host + ":" + port);
        } else {
            VSProtocol.LOGGER.fine("VSClient connected to server " + host + ":" + port);
        }

        return socket;
    }

}