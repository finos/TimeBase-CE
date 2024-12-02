/*
 * Copyright 2024 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.comm.cat.TomcatRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeBaseServerRegistry;
import com.epam.deltix.qsrv.hf.tickdb.test.EmbeddedServer;

import java.net.ServerSocket;

/**
 * Tomcat Test Server
 */
public class TomcatServer implements EmbeddedServer {
    private TomcatRunner runner;
    private StartConfiguration config;
    private int             port;
    private int             webPort;
    private Boolean enableAeron = false; // Aeron is disabled for TomcatServer by default

    public TomcatServer() {
        this(null, 0);
    }

    public TomcatServer(StartConfiguration config) {
        this.config = config;
        this.port = config != null ? config.port : 0;
    }

    public TomcatServer(StartConfiguration config, int port) {
        this.config = config;
        this.port = port;
    }

    public TomcatServer (StartConfiguration config, int port, int webPort, boolean enableAeron) {
        this.config = config;
        this.port = port;
        this.webPort = webPort;
        this.enableAeron = enableAeron;
    }

    @Override
    public int start() throws Exception {

        if (config == null)
            config = StartConfiguration.create(true, false, false);

        if (enableAeron != null) {
            config.tb.getProps().setProperty(DXServerAeronContext.SYS_PROP_TIME_BASE_AERON_ENABLED, Boolean.toString(enableAeron));
        }

        config.port = port;

        if (config.port == 0 || config.port == -1) {
            ServerSocket socket = new ServerSocket();
            socket.bind(null);
            config.port = socket.getLocalPort();
            config.tb.setPort(config.port);
            socket.close();
        }

        if (webPort == 0) {
            ServerSocket socket = new ServerSocket();
            socket.bind(null);
            config.webPort = socket.getLocalPort();
            config.tb.setWebPort(config.webPort);
            socket.close();
        }

        runner = new TomcatRunner(config);
        runner.run();

        return config.port;
    }

    @Override
    public void stop() {
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

    @Override
    public int getWebPort() {
        return config.webPort;
    }
}