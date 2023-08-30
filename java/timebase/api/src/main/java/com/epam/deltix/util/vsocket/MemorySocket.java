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

import com.epam.deltix.util.collections.ByteQueue;
import com.epam.deltix.util.io.ByteQueueInputStream;
import com.epam.deltix.util.io.ByteQueueOutputStream;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *
 */
public class MemorySocket implements VSocket {

    ByteQueue q;
    private InputStream in;
    private BufferedInputStream bin;

    private OutputStream out;
    private VSocketOutputStream  vout;
    private VSocketInputStream   vin;
    private int socketNumber;

    public MemorySocket(int socketNumber) {
        this.socketNumber = socketNumber;
        q = new ByteQueue(1024 * 1024 * 10);
        in = new ByteQueueInputStream(q);
        bin = new BufferedInputStream(in);
        vin = new VSocketInputStream(bin, getSocketIdStr());
    }

    public MemorySocket(MemorySocket remote, int socketNumber) {
        this(socketNumber);

        remote.out = new ByteQueueOutputStream((ByteQueueInputStream) in);
        remote.vout = new VSocketOutputStream(remote.out, getSocketIdStr());

        this.out = new ByteQueueOutputStream((ByteQueueInputStream) remote.in);
        this.vout = new VSocketOutputStream(out, getSocketIdStr());
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
    public String getRemoteAddress() {
        return null;
    }

    @Override
    public void close() {
        
    }

    @Override
    public int getCode() {
        return hashCode();
    }

    @Override
    public void                         setCode(int code) {
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