package com.epam.deltix.qsrv.jetty;

import com.epam.deltix.qsrv.SSLConfig;
import com.epam.deltix.qsrv.SSLProperties;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.config.QuantServiceConfig;
import com.epam.deltix.qsrv.util.servlet.AccessFilter;
import com.epam.deltix.util.io.Home;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.apache.jsp.JettyJasperInitializer;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContainerInitializerHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.epam.deltix.qsrv.config.QuantServiceConfig.ENABLE_REMOTE_ACCESS;

public class JettyRunner  {

    protected static final Logger LOGGER = Logger.getLogger (JettyRunner.class.getName());

    public static final String  DEFAULT_WEB_APP_DIR = "default"; //QuantServer/web/default
    public static final String  DEFAULT_WEB_APP_NAME = ""; // must be empty string

    public static final String  TIME_BASE_WEB_APP_NAME    = "tb";
    public static final String  AGGREGATOR_WEB_APP_NAME   = "agg";

    // UHF and derived web apps use the same webapp name (which allows RPC/HTTP clients to use single dispatch servlet path)
    public static final String  UHF_WEB_APP_NAME          = "uhf";
    public static final String  ES_WEB_APP_NAME           = UHF_WEB_APP_NAME;
    public static final String STS_WEB_APP_NAME           = UHF_WEB_APP_NAME;

    public static final String  STS_WEB_APP_DIR           = "sts";
    private static final String  ES_WEB_APP_DIR           = "es";
    private static final String  UHF_WEB_APP_DIR          = "uhf";

    // TODO: MODULARIZATION
    public static final String UHF_TRADE_CTX = "config/trade.xml";

    private Server jettyServer;
    private final StartConfiguration config;

    public JettyRunner(StartConfiguration config) {
        this.config = config;
    }

    public void             init() throws Exception {
        setRemoteAccess();

        if (jettyServer != null)
            throw new IllegalStateException("Jetty already initialized");

        jettyServer = new Server();

        List<ConnectionFactory> factoryList = new ArrayList<>();
        // Create the HTTP/1.1 ConnectionFactory.
        HttpConnectionFactory http = new HttpConnectionFactory();
        SSLProperties sslConfig = getSslConfig();
        if (sslConfig != null && sslConfig.enableSSL) {
            // Create and configure the TLS context factory.
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(sslConfig.keystoreFile);
            sslContextFactory.setKeyStorePassword(sslConfig.keystorePass);

            // Create the TLS ConnectionFactory,
            // setting HTTP/1.1 as the wrapped protocol.
            SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, http.getProtocol());

            // Create the detector ConnectionFactory to
            // detect whether the initial bytes are TLS.
            DetectorConnectionFactory tlsDetector = new DetectorConnectionFactory(tls);
            factoryList.add(tlsDetector);
        }
        factoryList.add(http);

        ServerConnector connector = new ServerConnector(jettyServer, factoryList.toArray(new ConnectionFactory[0]));
        connector.setPort(config.port);
        jettyServer.addConnector(connector);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        List<Handler> handlerList = new ArrayList<>();
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/" + DEFAULT_WEB_APP_NAME);
        webapp.setWar(getWebappFile(config.quantServer, Home.getFile ("web/" + DEFAULT_WEB_APP_DIR)).getAbsolutePath());
//        webapp.addServletContainerInitializer(new ServletContainerInitializerHolder(JettyJasperInitializer.class));
//        webapp.setConfigurations(new Configuration[] {
//                new AnnotationConfiguration(),
//                new WebXmlConfiguration()
//        });
        handlerList.add(webapp);
        if (config.tb != null) {
            webapp = new WebAppContext();
            webapp.setContextPath("/" + TIME_BASE_WEB_APP_NAME);
            webapp.setWar(getWebappFile(config.tb, Home.getFile ("web/" + TIME_BASE_WEB_APP_NAME)).getAbsolutePath());
            handlerList.add(webapp);
        }

        Handler [] handlers = new Handler[handlerList.size()];
        contexts.setHandlers(handlerList.toArray(handlers));
        jettyServer.setHandler(contexts);
    }

    private SSLProperties getSslConfig() {
        if (config.tb != null)
            return config.tb.getSSLConfig();
        else if (config.uhf != null)
            return config.uhf.getSSLConfig();
        else if (config.es != null)
            return config.es.getSSLConfig();
        return null;
    }

    // Destroy instance of tomcat
    private  void             destroy() {
        if (jettyServer != null) {
            jettyServer.destroy();
            jettyServer = null;
        }
    }

    //just starting server
    private void             start() throws Exception {
        if (jettyServer != null)
            jettyServer.start();
    }

    // just stopping server
    private void             stop() throws Exception {
        if (jettyServer != null)
            jettyServer.stop();
    }

    /*
       Init and run server
     */
    public void             run() throws Exception {
        if (jettyServer == null)
            init();
        start();
    }

    /*
        Stops and destroy server
    */
    public void             close() {
        // let's stop servicing incoming requests as a first step
        try {
            stop();
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }

        try {
            destroy();
        } catch (Throwable e) {
            LOGGER.log (Level.SEVERE, e.getMessage (), e);
        }
    }

    public void waitForStop() throws InterruptedException {
        jettyServer.join();
    }

    private QuantServiceConfig  getQSConfig() {
        if (config == null)
            return null;

        if (config.tb != null)
            return config.tb;
        if (config.agg != null)
            return config.agg;
        if (config.uhf != null)
            return config.uhf;
        if (config.es != null)
            return config.es;
        if (config.sts != null)
            return config.sts;

        return null;
    }

    private static File getWebappFile(QuantServiceConfig config, File def) {
        String webappPath = config.getString(QuantServiceConfig.WEBAPP_PATH);
        return webappPath != null ? new File(webappPath) : def;
    }

    private void            setRemoteAccess() {
        if (System.getProperty(AccessFilter.ENABLE_REMOTE_ACCESS_PROP) == null) {
            QuantServiceConfig qsConfig = getQSConfig();
            if (qsConfig != null) {
                boolean enable = qsConfig.getBoolean(ENABLE_REMOTE_ACCESS, true);
                System.setProperty(AccessFilter.ENABLE_REMOTE_ACCESS_PROP, String.valueOf(enable));
            }
        }
    }
}
