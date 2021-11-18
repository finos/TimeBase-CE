package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.comm.cat.ServiceExecutorBootstrap;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.config.QuantServerExecutor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeBaseServerRegistry;
import com.epam.deltix.qsrv.hf.tickdb.test.EmbeddedServer;
import com.epam.deltix.util.vsocket.VSServerRunner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class JettyServer implements EmbeddedServer {

    private VSServerRunner vsServerRunner;

    private final ServiceExecutorBootstrap bootstrap;

    private final StartConfiguration config;

    public JettyServer(StartConfiguration config) {
        this.config = config;
        bootstrap = new ServiceExecutorBootstrap(config);
    }

    @Override
    public int start() throws Exception {
        allocatePort();
        bootstrap.start();
        vsServerRunner = new VSServerRunner(config.port, InetAddress.getLoopbackAddress());
        vsServerRunner.init(QuantServerExecutor.HANDLER);
        vsServerRunner.start();
        return config.port;
    }

    @Override
    public void stop() throws Exception {
        vsServerRunner.close();
        bootstrap.close();
    }

    @Override
    public DXTickDB getDB() {
        return TimeBaseServerRegistry.getServer(config.port);
    }

    @Override
    public int getPort() {
        return config.port;
    }

    private void allocatePort() throws IOException {
        if (config.port == 0 || config.port == -1) {
            try (ServerSocket socket = new ServerSocket()){
                socket.bind(null);
                config.port = socket.getLocalPort();
                config.tb.setPort(config.port);
            }
        }
    }
}
