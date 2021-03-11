package com.epam.deltix.util.vsocket;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

/**
 *
 */
public class VSServerSocketFactory {
    public static ServerSocket                    createServerSocket(int port) throws IOException {
        return createServerSocket(port, null, null);
    }

    public static ServerSocket                    createServerSocket(int port, InetAddress address) throws IOException {
        return createServerSocket(port, address, null);
    }

    public static ServerSocket                    createServerSocket(int port, InetAddress address, TLSContext ctx) throws IOException {
        ServerSocket socket = null;
        if (ctx != null) {
            //SSL server socket
            SSLServerSocketFactory ssf = ctx.context.getServerSocketFactory();
            socket = ssf.createServerSocket(port, 0, address);
        } else {
            socket = new ServerSocket(port, 0, address); // if backlog = 0 server will decide automatically
        }

        return socket;
    }

}
