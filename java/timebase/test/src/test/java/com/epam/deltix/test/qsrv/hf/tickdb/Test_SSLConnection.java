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
