package com.epam.deltix.qsrv.comm.cat;

import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.tomcat.ConnectionHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketServer extends Thread implements Disposable {

    public static final Logger LOGGER = Logger.getLogger ("deltix.tickdb.server");

    private final ConnectionHandler connectionHandler;
    private final ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private volatile boolean running;

    public SocketServer(ConnectionHandler connectionHandler) throws IOException {
        this(0, connectionHandler);
    }

    public SocketServer(int port, ConnectionHandler connectionHandler) throws IOException {
        this(port, null, connectionHandler);
    }

    public SocketServer(int port, InetAddress address, ConnectionHandler connectionHandler) throws IOException {
        this(new ServerSocket(port, 0, address), connectionHandler);
    }

    public SocketServer(ServerSocket serverSocket, ConnectionHandler connectionHandler) {
        super("VSServer on " + serverSocket);

        this.serverSocket = serverSocket;
        this.connectionHandler = connectionHandler;
    }

    public int getLocalPort() {
        return (serverSocket.getLocalPort());
    }

    public int getSoTimeout() throws IOException {
        return serverSocket.getSoTimeout();
    }

    public void setSoTimeout(int readTimeout) throws SocketException {
        serverSocket.setSoTimeout(readTimeout);
    }

    @Override
    public void run() {
        running = true;

        LOGGER.log(Level.INFO, "Listening connections on port: " + serverSocket.getLocalPort());

        while (running) {
            try {
                final Socket s = serverSocket.accept();

                executor.execute(() -> {
                    try {
                        BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
                        OutputStream os = s.getOutputStream();
                        if (!connectionHandler.handleConnection(s, bis, os)) {
                            s.close();
                        }
                    } catch (Throwable t) {
                        IOUtil.close(s);
                        LOGGER.log(
                            Level.SEVERE,
                            "Exception while handling handshake",
                            t
                        );
                    }
                });
            } catch (IOException iox) {
                if (!serverSocket.isClosed())
                    LOGGER.log(
                        Level.SEVERE,
                        "Exception while accepting connections",
                        iox
                    );
            }
        }

        if (!serverSocket.isClosed())
            IOUtil.close(serverSocket);
    }

    @Override
    public void close() {
        running = false;
        IOUtil.close(serverSocket);
        executor.shutdownNow();
        interrupt();
    }
}
