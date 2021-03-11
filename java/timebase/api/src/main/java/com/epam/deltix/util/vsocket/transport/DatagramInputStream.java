package com.epam.deltix.util.vsocket.transport;

/**
 *
 */

import com.epam.deltix.util.collections.ByteQueue;
import com.epam.deltix.util.memory.DataExchangeUtils;

import java.io.*;
import java.net.*;

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
            e.printStackTrace(System.out);
            return false;
        }

        int s = DataExchangeUtils.readInt(pack.getData(), 0);
        //System.out.println("sequence: " + s);
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