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

/**
 *
 */

import com.epam.deltix.util.collections.ByteQueue;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.vsocket.VSProtocol;

import java.io.*;
import java.net.*;
import java.util.logging.Level;

public class DatagramInputStream extends InputStream {
    private ByteQueue       buffer = new ByteQueue(1024 * 1024);

    private DatagramSocket ds;
    private DatagramConnection connection;

    private byte[]          data = new byte[1024 * 64];

    public DatagramInputStream(DatagramConnection connection) {
        this.connection = connection;
        this.ds = connection.socket;
    }

    private boolean fillBuffer() {
        
        DatagramPacket pack = new DatagramPacket(data, data.length);
        try {
            ds.receive(pack);
            if (connection.remotePort == -1)
                connection.remotePort = pack.getPort();

        } catch (Exception e) {
            VSProtocol.LOGGER.log (Level.WARNING, null, e);
            return false;
        }

        int s = DataExchangeUtils.readInt(pack.getData(), 0);
        buffer.offer(pack.getData(), 4, pack.getLength() - 4);

        return true;
    }

    public boolean markSupported() {
        return false;
    }

    public int  read() {

        if (!fillBuffer())
            return -1;

        return buffer.poll();
    }

    public int read(byte buf[]) {

        return read(buf, 0, buf.length);
    }

    public int read(byte buf[], int pos, int len) {

        if (!fillBuffer())
            return -1;

        int count = Math.min(len, buffer.size());
        
        buffer.poll(buf, pos, count);

        return count;
    }
}