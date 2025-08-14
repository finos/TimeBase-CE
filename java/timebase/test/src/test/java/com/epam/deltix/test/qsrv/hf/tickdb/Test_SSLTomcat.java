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

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.SSLProperties;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.servlet.HomeServlet;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.SSLClientContextProvider;
import com.epam.deltix.util.net.SSLContextProvider;
import org.junit.*;

import static com.epam.deltix.qsrv.hf.tickdb.TDBRunner.testHome;
import static junit.framework.Assert.assertEquals;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

import java.io.File;
import java.io.IOException;

/**
 *
 */
@Category(TickDBFast.class)
public class Test_SSLTomcat {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        SSLClientContextProvider.useDynamicKeystore(true);

        File tb = new File(TDBRunner.getTemporaryLocation());
        QSHome.set(tb.getParent());

        File certificate = new File(tb.getParent(), "selfsigned.jks");
        IOUtil.extractResource("com/epam/deltix/cert/selfsigned.jks", certificate);

        StartConfiguration config = StartConfiguration.create(true, false, false);
        SSLProperties ssl = new SSLProperties(true, false);
        ssl.keystoreFile = certificate.getAbsolutePath();
        ssl.keystorePass = "deltix";

        config.tb.setSSLConfig(ssl);

        System.setProperty(SSLClientContextProvider.CLIENT_KEYSTORE_PROPNAME, certificate.getAbsolutePath());
        System.setProperty(SSLClientContextProvider.CLIENT_KEYSTORE_PASS_PROPNAME, "deltix");

        runner = new TDBRunner(true, true, tb.getAbsolutePath(), new TomcatServer(config));
        runner.startup();
    }

    @Test
    public void testHomeServletSSL() throws Throwable {
        testHome("localhost", runner.getWebPort(), new File(runner.getLocation()).getParent());
    }

    @AfterClass
    public static void stop() throws Throwable {
        SSLClientContextProvider.useDynamicKeystore(false);
        runner.shutdown();
        runner = null;
    }

    @Test
    public void testConnectionToSSLTomcat() throws Throwable {
        DXTickDB client = TickDBFactory.connect("localhost", runner.getPort(), false);
        client.open(false);
        Assert.assertEquals(((TickDBClient) client).isSSLEnabled(), false);
        client.close();

        //connect with ssl
        DXTickDB sslClient = TickDBFactory.connect("localhost", runner.getPort(), true);
        sslClient.open(false);
        Assert.assertEquals(((TickDBClient) sslClient).isSSLEnabled(), true);
        sslClient.close();
    }
}