package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.tomcat.ConnectionHandler;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VSServerRunner extends Thread {

    public static final Logger LOGGER = Logger.getLogger(VSServerRunner.class.getName());

    private ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newFixedThreadPool(100);

    private final int port;
    private final InetAddress address;

    private ConnectionHandler connectionHandler;

    public VSServerRunner(int port, InetAddress address) {
        this.address = address;
        this.port = port;
    }

    public void init(ConnectionHandler connectionHandler) {
        try {
            this.serverSocket = VSServerSocketFactory.createServerSocket(port, address);
        } catch (IOException iox) {
            LOGGER.log(Level.SEVERE, "Failed to start VSServerRunner", iox);
            throw new RuntimeException(iox);
        }
        this.connectionHandler = connectionHandler;
    }

    @Override
    public void run() {
        for (;;) {
            try {
                final Socket socket = serverSocket.accept();
                executor.execute(() -> {
                    try {
                        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                        if (!connectionHandler.handleConnection(socket, bis, socket.getOutputStream()))
                            socket.close();
                    } catch (IOException e) {
                        IOUtil.close(socket);
                    }
                });
            } catch (IOException iox) {
                if (!serverSocket.isClosed()) {
                    VSProtocol.LOGGER.log(Level.WARNING, "Exception while accepting connections", iox);
                } else {
                    break;
                }
            }
        }

        if (serverSocket != null && !serverSocket.isClosed())
            IOUtil.close(serverSocket);
        connectionHandler.close();
    }

    public void close() {
        IOUtil.close(serverSocket);
        interrupt();
        executor.shutdown();
        if (connectionHandler != null)
           connectionHandler.close();
    }
}
