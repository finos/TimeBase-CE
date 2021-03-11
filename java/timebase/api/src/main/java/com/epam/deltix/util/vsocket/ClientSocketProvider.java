package com.epam.deltix.util.vsocket;

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 
 */
public class ClientSocketProvider {

    public static Socket        open(SocketFactory factory, String host, int port, int connectTimeout, int timeout) throws IOException {
        //create and connect
        Socket socket = factory.createSocket();

        socket.setSoTimeout(timeout);
        socket.connect(new InetSocketAddress(host, port), connectTimeout);

        if (socket instanceof SSLSocket) {
            //do ssl handshake
            ((SSLSocket) socket).startHandshake();
            VSProtocol.LOGGER.fine("VSClient connected to SSL server " + host + ":" + port);
        } else {
            VSProtocol.LOGGER.fine("VSClient connected to server " + host + ":" + port);
        }

        return socket;
    }

}
