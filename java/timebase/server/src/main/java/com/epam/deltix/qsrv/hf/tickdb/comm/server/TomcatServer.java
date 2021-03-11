package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.comm.cat.TomcatRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeBaseServerRegistry;
import com.epam.deltix.qsrv.hf.tickdb.test.EmbeddedServer;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Created by Alex Karpovich on 4/4/2018.
 */
public class TomcatServer implements EmbeddedServer {
    private TomcatRunner        runner;
    private StartConfiguration  config;
    private int                 port;

    public TomcatServer() {
        this(null, 0);
    }

    public TomcatServer(StartConfiguration config) {
        this.config = config;
        this.port = config != null ? config.port : 0;
    }

    public TomcatServer (StartConfiguration config, int port) {
        this.config = config;
        this.port = port;
    }

    @Override
    public int start () throws Exception {

        if (config == null)
            config = StartConfiguration.create(true, false, false);

        config.port = port;

        if (config.port == 0 || config.port == -1) {
            ServerSocket socket = new ServerSocket();
            socket.bind(null);
            config.port = socket.getLocalPort();
            config.tb.setPort(config.port);
            socket.close();
        }

        runner = new TomcatRunner(config);
        runner.run();

        return config.port;
    }

    @Override
    public void stop () {
        if (runner != null)
            runner.close();
    }

    @Override
    public DXTickDB getDB() {
        return TimeBaseServerRegistry.getServer(config.port);
    }


    @Override
    public int getPort() {
        return config.port;
    }
}

