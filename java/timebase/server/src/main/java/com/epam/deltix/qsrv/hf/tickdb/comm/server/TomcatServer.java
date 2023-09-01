/*
 * Copyright 2023 EPAM Systems, Inc
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
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeBaseServerRegistry;
import com.epam.deltix.qsrv.hf.tickdb.test.EmbeddedServer;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Tomcat Test Server
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