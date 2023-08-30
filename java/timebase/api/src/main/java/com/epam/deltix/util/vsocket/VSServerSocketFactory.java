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

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 *
 */
public class VSServerSocketFactory {
    public static ServerSocket                    createServerSocket(int port) throws IOException {
        return createServerSocket(port, null, null);
    }

    public static ServerSocket                    createServerSocket(int port, InetAddress address) throws IOException {
        return createServerSocket(port, address, null);
    }

    public static ServerSocket                    createServerSocket(int port, InetAddress address, TLSContext ctx) throws IOException {
        ServerSocket socket = null;
        if (ctx != null) {
            //SSL server socket
            SSLServerSocketFactory ssf = ctx.context.getServerSocketFactory();
            socket = ssf.createServerSocket(port, 0, address);
        } else {
            socket = new ServerSocket(port, 0, address); // if backlog = 0 server will decide automatically
        }

        return socket;
    }

}