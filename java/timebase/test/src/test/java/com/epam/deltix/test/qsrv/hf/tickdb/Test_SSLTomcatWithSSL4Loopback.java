package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.SSLProperties;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.SSLClientContextProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 *
 */
@Category(TickDBFast.class)
public class Test_SSLTomcatWithSSL4Loopback {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {

        File tb = new File(TDBRunner.getTemporaryLocation());
        QSHome.set(tb.getParent());

        File certificate = new File(tb.getParent(), "selfsigned.jks");
        IOUtil.extractResource("com/epam/deltix/cert/selfsigned.jks", certificate);

        SSLProperties ssl = new SSLProperties(true, true);
        ssl.keystoreFile = certificate.getAbsolutePath();

        System.setProperty(SSLClientContextProvider.CLIENT_KEYSTORE_PROPNAME, ssl.keystoreFile);
        System.setProperty(SSLClientContextProvider.CLIENT_KEYSTORE_PASS_PROPNAME, ssl.keystorePass);

        StartConfiguration configuration = StartConfiguration.create(true, false, false);
        configuration.tb.setSSLConfig(ssl);

        runner = new TDBRunner(true, true, tb.getAbsolutePath(), new TomcatServer(configuration));
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
        assertEquals(true, ((TickDBClient) client).isSSLEnabled()); //becomes SSL for this test
        client.close();

        //connect with ssl
        DXTickDB sslClient = TickDBFactory.connect("localhost", runner.getPort(), true);
        sslClient.open(false);
        assertEquals(true, ((TickDBClient) sslClient).isSSLEnabled());
        sslClient.close();
    }
}
