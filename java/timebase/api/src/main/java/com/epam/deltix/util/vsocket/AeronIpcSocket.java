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

import com.epam.deltix.util.io.aeron.DXAeron;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.lang.Util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 *
 */
public class AeronIpcSocket implements VSocket {
    private Socket                      socket;
    private InputStream                 in;
    private OutputStream                out;
    private VSocketInputStream          vin;
    private VSocketOutputStream         vout;
    private int                         code;
    private int socketNumber;

    public AeronIpcSocket(Socket socket, int code, boolean isServer, int socketNumber) {
        this.socket = socket;
        this.code = code;
        this.socketNumber = socketNumber;

        if (isServer) {
            in = DXAeron.createInputStream(code);
            out = DXAeron.createOutputStream(code + 1);
        } else {
            in = DXAeron.createInputStream(code + 1);
            out = DXAeron.createOutputStream(code);
        }

        vin = new VSocketInputStream(in, getSocketIdStr());
        vout = new VSocketOutputStream(out, getSocketIdStr());
    }

    @Override
    public VSocketInputStream          getInputStream() {
        return vin;
    }

    @Override
    public VSocketOutputStream         getOutputStream() {
        return vout;
    }

    @Override
    public String                      getRemoteAddress() {
        return String.valueOf(code);
    }

    @Override
    public void                        close() {
        Util.close(in);
        Util.close(out);
        Util.close(vin);
        Util.close(vout);
        IOUtil.close(socket);
    }

    @Override
    public int                         getCode() {
        return code;
    }

    @Override
    public void                        setCode(int code) {
        this.code = code;
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
