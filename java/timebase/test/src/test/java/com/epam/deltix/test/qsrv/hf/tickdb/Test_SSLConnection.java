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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *
 */
@Category(TickDBFast.class)
public class Test_SSLConnection {
    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new TDBRunner(true, true, new TomcatServer());
        runner.startup();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void testSSLConnectionToTomcat() throws Throwable {
        try (DXTickDB client = TickDBFactory.connect("localhost", runner.getPort(), false)) {
            client.open(false);
            assertEquals(((TickDBClient) client).isSSLEnabled(), false);
        }

        //connect with ssl
        try {
            try (DXTickDB sslClient = TickDBFactory.connect("localhost", runner.getPort(), true)) {
                sslClient.open(false);
                assertEquals(((TickDBClient) sslClient).isSSLEnabled(), true);
            }
            assertTrue(false);

        } catch (Exception t) {
            //ok situation
        }
    }
}