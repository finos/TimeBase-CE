package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.vsocket.util.SocketTestUtilities;
import com.epam.deltix.util.vsocket.util.TestVServerSocketFactory;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by PosudevskiyK on 1/24/2017.
 */
public class Test_VSocketInputThroughput {
    public static void main (String [] args) throws Exception {
        int port = SocketTestUtilities.parsePort(args);
        int packetSize = SocketTestUtilities.parsePacketSize(args);

        VSServer server = TestVServerSocketFactory.createInputThroughputVServer(port, packetSize);
        server.setDaemon(true);
        server.start();
        System.out.println("Server started on " + server.getLocalPort());

        client ("localhost", server.getLocalPort(), packetSize, 15);
        server.close();
    }

    public static void  client (String host, int port, int packetSize, int cycles)
            throws IOException {
        byte[] buffer = new byte[packetSize];

        for (int ii = 0; ii < packetSize; ii++)
            buffer[ii] = (byte) ii;

        VSClient c = new VSClient(host, port);
        c.connect();

        VSChannel s = c.openChannel();
        InputStream os = s.getInputStream();

        SocketTestUtilities.measureInputThroughput(os, buffer, cycles);
        c.close();
    }

    @Test
    public void TestOutputThroughput() throws Throwable {
        Test_VSocketInputThroughput.main(new String[0]);
    }
}
