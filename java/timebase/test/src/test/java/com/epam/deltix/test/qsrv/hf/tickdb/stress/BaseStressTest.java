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
package com.epam.deltix.test.qsrv.hf.tickdb.stress;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Base class for tests that may be executed against local embedded TB server OR external remote server.
 *
 * @author Alexei Osipov
 */
@SuppressWarnings("ConstantConditions")
public class BaseStressTest {
    private static final String REMOTE_SERVER_HOST = null; //"3.14.6.93";
    private static final int REMOTE_SERVER_PORT = 8011;

    private static final boolean USE_EMBEDDED = REMOTE_SERVER_HOST == null;


    protected static TDBRunner runner;

    protected DXTickDB createClient() {
        if (USE_EMBEDDED) {
            return runner.createClient();
        } else {
            return TickDBFactory.connect(REMOTE_SERVER_HOST, REMOTE_SERVER_PORT, false);
        }
    }

    @BeforeClass
    public static void start() throws Throwable {
        if (USE_EMBEDDED) {
            runner = new TDBRunner(true, true, TDBRunner.getTemporaryLocation(), new TomcatServer());
            runner.startup();
        }
    }

    @AfterClass
    public static void stop() throws Throwable {
        if (USE_EMBEDDED) {
            runner.shutdown();
            runner = null;
        }
    }
}
