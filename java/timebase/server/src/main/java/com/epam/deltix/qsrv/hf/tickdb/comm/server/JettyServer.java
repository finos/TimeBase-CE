package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.comm.cat.ServiceExecutorBootstrap;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.config.QuantServerExecutor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeBaseServerRegistry;
import com.epam.deltix.qsrv.hf.tickdb.test.EmbeddedServer;
import com.epam.deltix.qsrv.jetty.JettyRunner;
import com.epam.deltix.util.vsocket.VSServerRunner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class JettyServer implements EmbeddedServer {

    private VSServerRunner vsServerRunner;

    private ServiceExecutorBootstrap bootstrap;

    private StartConfiguration config;

    private JettyRunner jettyRunner;

    @Override
    public int start() throws Exception {
        config = StartConfiguration.create(true, false, false);
        bootstrap = new ServiceExecutorBootstrap(config);
        allocatePorts();
        bootstrap.start();
        vsServerRunner = new VSServerRunner(config.port, InetAddress.getLoopbackAddress());
        vsServerRunner.init(QuantServerExecutor.HANDLER);
        vsServerRunner.start();
        jettyRunner = new JettyRunner(config);
        jettyRunner.run();
        return config.port;
    }

    @Override
    public void stop() throws Exception {
        jettyRunner.close();
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

    @Override
    public int getHttpPort() {
        return config.httpPort;
    }

    private void allocatePorts() throws IOException {
        if (config.port == 0 || config.port == -1) {
            try (ServerSocket socket = new ServerSocket()){
                socket.bind(null);
                config.port = socket.getLocalPort();
                config.tb.setPort(config.port);
            }
        }
        if (config.httpPort == 0 || config.httpPort == -1) {
            try (ServerSocket socket = new ServerSocket()){
                socket.bind(null);
                config.httpPort = socket.getLocalPort();
            }
        }
    }
}
