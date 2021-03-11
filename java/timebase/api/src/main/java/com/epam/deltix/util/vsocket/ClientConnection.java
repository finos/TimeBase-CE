package com.epam.deltix.util.vsocket;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author Alexei Osipov
 */
public class ClientConnection {
    private final Socket socket;

    private final InputStream in;
    private final OutputStream os;

    private final BufferedInputStream bin;

    public ClientConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
        this.os = socket.getOutputStream();
        this.bin = new BufferedInputStream(this.in);
    }

    public Socket getSocket() {
        return socket;
    }

    public InputStream getInputStream() {
        return in;
    }

    public BufferedInputStream getBufferedInputStream() {
        return bin;
    }

    public OutputStream getOutputStream() {
        return os;
    }
}
