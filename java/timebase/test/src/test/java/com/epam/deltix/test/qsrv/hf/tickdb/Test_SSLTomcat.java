package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.SSLProperties;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

import java.io.File;

/**
 *
 */
@Category(TickDBFast.class)
public class Test_SSLTomcat {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        File tb = new File(TDBRunner.getTemporaryLocation());
        QSHome.set(tb.getParent());

        StartConfiguration config = StartConfiguration.create(true, false, false);
        config.tb.setSSLConfig(new SSLProperties(true, false));
        runner = new TDBRunner(true, true, tb.getAbsolutePath(), new TomcatServer(config));
        runner.startup();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void testConnectionToSSLTomcat() throws Throwable {
        DXTickDB client = TickDBFactory.connect("localhost", runner.getPort(), false);
        client.open(false);
        assertEquals(((TickDBClient) client).isSSLEnabled(), false);
        client.close();

        //connect with ssl
        DXTickDB sslClient = TickDBFactory.connect("localhost", runner.getPort(), true);
        sslClient.open(false);
        assertEquals(((TickDBClient) sslClient).isSSLEnabled(), true);
        sslClient.close();
    }
}
