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

import com.epam.deltix.util.memory.MemoryDataOutput;

import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 *
 */
public class DatagramOutputStream extends OutputStream {
    private byte[]          buffer;
    private DatagramSocket  ds;

    private int sequence = 0;
    private DatagramConnection c;
    private MemoryDataOutput out = new MemoryDataOutput();
    DatagramPacket packet = new DatagramPacket(out.getBuffer(), 0, 0);

    public DatagramOutputStream(DatagramConnection connection, InetAddress ip) {
        this.c = connection;

        this.ds = connection.socket;
        packet.setAddress(ip);
    }

    public synchronized void     write(int b) {
        out.reset();
        out.writeInt(sequence++);
        out.writeByte(b);

        packet.setData(out.getBuffer(), 0, out.getSize());
        packet.setPort(c.remotePort);

        try {
            ds.send(packet);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    public  void     write(byte buf[]) {
        write(buf, 0, buf.length);
    }

    public synchronized void     write(byte buf[], int pos, int len) {
        if (len == 0)
            return;

        out.reset();
        out.writeInt(sequence++);
        out.write(buf, pos,len);

        packet.setData(out.getBuffer(), 0, out.getSize());
        packet.setPort(c.remotePort);

        try {
            ds.send(packet);
            //System.out.println("Send packet, size = " + out.getSize());
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
}
