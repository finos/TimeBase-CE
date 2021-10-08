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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.SSLProperties;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.servlet.HomeServlet;
import com.epam.deltix.util.net.SSLContextProvider;
import org.junit.*;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_TomcatServer {

    @Test
    public void testHomeServlet() throws Throwable {
        TDBRunner runner = new TDBRunner(true, true, new TomcatServer(null));
        runner.startup();

        testHome("localhost", runner.getPort(), new File(runner.getLocation()).getParent());

        runner.shutdown();
    }

    @Test
    public void testHomeServletSSL() throws Throwable {

        StartConfiguration configuration = StartConfiguration.create(true, false, false);
        configuration.tb.setSSLConfig(new SSLProperties(true, false));

        TDBRunner runner = new TDBRunner(true, true, new TomcatServer(configuration));
        //runner.sslContext = SSLContextProvider.createSSLContext(ssl.keystoreFile, ssl.keystorePass, false);
        runner.startup();

        testHome("localhost", runner.getPort(), new File(runner.getLocation()).getParent());

        runner.shutdown();
    }

    public static void testHome(String host, int port, String home) throws IOException {
        String value = HomeServlet.get(host, port);
        assertEquals(home, value);
    }

}
