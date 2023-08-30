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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author Alexei Osipov
 */
public class ClientConnection {
    private final Socket socket;

    private final InputStream in;
    private final OutputStream os;

    private final BufferedInputStream bin;

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.os = socket.getOutputStream();
        this.bin = new BufferedInputStream(this.in);
    }

    public Socket getSocket() {
        return socket;
    }

    public InputStream getInputStream() {
        return in;
    }

    public BufferedInputStream getBufferedInputStream() {
        return bin;
    }

    public OutputStream getOutputStream() {
        return os;
    }
}