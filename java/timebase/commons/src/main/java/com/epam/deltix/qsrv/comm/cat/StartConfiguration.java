/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.comm.cat;

import java.io.IOException;

import com.epam.deltix.qsrv.config.QuantServerExecutor;
import com.epam.deltix.qsrv.config.QuantServiceConfig;
import com.epam.deltix.qsrv.config.ServiceExecutor;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import static com.epam.deltix.qsrv.config.QuantServiceConfig.Type;

public class StartConfiguration {

    private static ObjectToObjectHashMap<Type, String> DEFAULTS = new ObjectToObjectHashMap<>();
    static {
        DEFAULTS.put(Type.TimeBase, "com.epam.deltix.qsrv.config.TimebaseServiceExecutor");
        DEFAULTS.put(Type.QuantServer, QuantServerExecutor.class.getName());
    }

    public QuantServiceConfig   tb;
    public QuantServiceConfig   quantServer;

    public int                  port;

    private ObjectToObjectHashMap<Type, ServiceExecutor> executors = new ObjectToObjectHashMap<>();

    public static StartConfiguration create(boolean timebase, boolean aggregator, boolean uhf) throws IOException {
        return create(timebase, aggregator, uhf, false, false, -1);
    }

    public static StartConfiguration create(boolean timebase, boolean aggregator, boolean es, boolean sts) throws IOException {
        return create(timebase, aggregator, false, es, sts, -1);
    }

    private static StartConfiguration create(boolean timebase, boolean aggregator, boolean uhf, boolean es, boolean sts, int port) throws IOException {
        StartConfiguration config = new StartConfiguration();
        config.port = port;

        config.tb = (timebase) ? QuantServiceConfig.forService(Type.TimeBase) : null;
        config.quantServer = QuantServiceConfig.forService(Type.QuantServer);

        return config;
    }

    public ServiceExecutor          getExecutor(Type type) {
        ServiceExecutor executor = executors.get(type, null);

        if (executor == null) {
            String clazz = DEFAULTS.get(type, null);

            if (clazz == null)
                throw new IllegalArgumentException("Service type: " + type + " is unknown");

            try {
                executors.put(type, executor = (ServiceExecutor) Class.forName(clazz).newInstance());
            } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return executor;
    }

    public void                     setExecutor(Type type, ServiceExecutor exe) {
        executors.put(type, exe);
    }

}