package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.vsocket.util.SocketTestUtilities;
import com.epam.deltix.util.vsocket.util.TestVServerSocketFactory;
import org.junit.Test;

import java.io.IOException;

public class Test_VSocketLatency {
    public static void main (String [] args) throws Exception {
        int port = SocketTestUtilities.parsePort(args);
        int packetSize = SocketTestUtilities.parsePacketSize(args, 1024);

        VSServer server = TestVServerSocketFactory.createLatencyVServer(port, packetSize);
        server.setDaemon(true);
        server.start();
        System.out.println("Server started on " + server.getLocalPort());

        client ("localhost", server.getLocalPort(), packetSize, 10);
    }

    public static void  client (String host, int port, int packetSize, int cycles)
            throws IOException, InterruptedException {
        byte[] buffer = new byte[packetSize];

        for (int ii = 0; ii < packetSize; ii++)
            buffer[ii] = (byte) ii;

        VSClient c = new VSClient(host, port);
        c.connect();

        VSChannel s = c.openChannel();
        s.setAutoflush(true);

        boolean measure = true;
        SocketTestUtilities.measureLatency(s.getDataOutputStream(), s.getDataInputStream(), buffer, cycles, measure);
    }

    @Test
    public void TestSocket() throws Throwable {
        Test_VSocketLatency.main(new String[0]);
    }
}
