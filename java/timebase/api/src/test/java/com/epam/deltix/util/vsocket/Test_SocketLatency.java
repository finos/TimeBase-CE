package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.vsocket.util.SocketTestUtilities;
import com.epam.deltix.util.vsocket.util.TestServerSocketFactory;
import org.junit.Test;

import java.io.*;
import java.net.Socket;

/**
 * Date: Mar 1, 2010
 */
public class Test_SocketLatency {
    public static void main (String [] args) throws Exception {
        int packetSize = SocketTestUtilities.parsePacketSize(args, 1024);
        int port = SocketTestUtilities.parsePort(args);

        TestServerSocketFactory.ServerThread server = TestServerSocketFactory.createLatencyServerSocket(port, packetSize);
        server.start ();

        client ("localhost", server.getLocalPort(), packetSize, 10, true);
    }

    public static void  client (String host, int port, int packetSize, int cycles, boolean measure)
            throws IOException, InterruptedException {
        byte[] buffer = new byte[packetSize];

        for (int ii = 0; ii < packetSize; ii++)
            buffer[ii] = (byte) ii;

        Socket socket = null;
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(0);
            socket.setKeepAlive(true);

            socket.setReuseAddress(true);

            OutputStream out = socket.getOutputStream();
            DataInputStream in = new DataInputStream(socket.getInputStream());

            SocketTestUtilities.measureLatency(out, in, buffer, cycles, measure);

        } catch (Throwable x) {
            x.printStackTrace();
            throw x;
        } finally {
            IOUtil.close(socket);
        }

        Thread.sleep(100);
    }

    @Test
    public void TestSocket() throws Exception {
        Test_SocketLatency.main(new String[0]);
    }
}


