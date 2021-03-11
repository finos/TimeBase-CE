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
