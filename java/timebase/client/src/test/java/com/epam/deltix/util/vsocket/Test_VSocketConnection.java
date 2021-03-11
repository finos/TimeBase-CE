package com.epam.deltix.util.vsocket;

import org.junit.Test;

import java.io.IOException;

public class Test_VSocketConnection {

    @Test
    public void testSocketClose() throws InterruptedException, IOException {

        VSServer server = new VSServer();

        server.setConnectionListener(
                (executor, serverChannel) -> {
                    //new ServerThread (executor, serverChannel).submit ();
                }
        );

        server.start();

        VSClient client = new VSClient("localhost", server.getLocalPort());
        client.connect();

        for (int i = 0; i < 10; i++) {
            VSChannel channel = client.openChannel();
            channel.close();
        }

        client.close();
        server.close();
    }
}
