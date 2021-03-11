package com.epam.deltix.util.vsocket.transport;

import com.epam.deltix.util.vsocket.TransportType;

import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 *
 */
public class SocketConnectionFactory {

    public static Connection createConnection(Socket s, BufferedInputStream bis, OutputStream os) {
        return new SocketConnection(s, bis, os);
    }
}
