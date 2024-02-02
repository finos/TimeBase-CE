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
package com.epam.deltix.qsrv.util.tomcat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.SSLProperties;
import com.epam.deltix.util.concurrent.Signal;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.tomcat.ConnectionHandler;
import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.*;

import com.epam.deltix.util.io.Home;
import org.apache.coyote.http11.Http11DXProtocol;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

/**
 *
 */
public class DXTomcat extends Tomcat {
    public static final Logger LOGGER = Logger.getLogger ("deltix.util.tomcat");

    private static final String ENGINE_NAME = "deltix-engine";
    private static final String DEFAULT_HOST_ID = "deltix-host";
    private static final String DX_CONNECTOR_CLASSNAME = "org.apache.coyote.http11.Http11DXProtocol";
    private static final int    TOMCAT_DEFAULT_PORT = 8011;

    private static final String CATALINA_PROPERTIES_FILE = "config/tomcat/catalina.properties";
    private static final String WEB_PROPERTIES_FILE = "config/tomcat/web.xml";

    private String              mHostName = null;

    private ConnectionHandler connectionHandler = null;

    private final String        webSubDir;
    private final File          webappFile;
    private final String        engineHost;
    private File                workDir;

    private final ClassLoader   parentClassLoader;
    private Context             defaultContext;

    private SSLProperties       sslConfig = null;

    private Signal              stopSignal = new Signal();

    public DXTomcat() {
        this ((String) null);
    }

    public DXTomcat(String subDir) {
        this(subDir, DEFAULT_HOST_ID);
    }

    public DXTomcat(File webappFile) {
        this(webappFile, DEFAULT_HOST_ID);
    }

    public DXTomcat(String subDir, String engineHost) {
        this(subDir, null, engineHost);
    }

    public DXTomcat(File webappFile, String engineHost) {
        this(null, webappFile, engineHost);
    }

    public DXTomcat(String subDir, File webappFile, String engineHost) {
        port = TOMCAT_DEFAULT_PORT;
        this.webSubDir = subDir;
        this.webappFile = webappFile;
        this.engineHost = engineHost;
        parentClassLoader = null;
    }

    public ConnectionHandler    getConnectionHandler () {
        return connectionHandler;
    }

    public void                 setConnectionHandler (ConnectionHandler connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    protected void              configureContext (Context context) {
        // Def do nothing
    }

    protected void              configureEngine (Engine engine) {
        // Def do nothing
    }

    public Context              getDefaultContext() {
        return defaultContext;
    }

    private  HashMap<String, Object> tomcatConfigs;
    public void setTomcatConfigs(HashMap<String, Object> tomcatConfigs){
        this.tomcatConfigs = tomcatConfigs;
    }

    public void                 init () throws LifecycleException {
        //init context dir
        File contextFile;
        if (webappFile != null) {
            contextFile = webappFile;
        } else {
            contextFile = webSubDir == null ? null : Home.getFile ("/web/" + webSubDir);
        }

        if (contextFile == null)
            throw new IllegalArgumentException("Web application configuration context directory cannot be established");

        if (! contextFile.exists())
            throw new IllegalArgumentException("Web application configuration is not found in \"" + contextFile.getAbsolutePath() + "\"");

        //init basedir
        String baseDir = getTomcatWorkDir();
        System.setProperty(Globals.CATALINA_BASE_PROP, baseDir);
        setBaseDir(baseDir);

        initCatalinaProperties();

        //init engine
        if (parentClassLoader != null) {
            getEngine().setParentClassLoader(parentClassLoader);
        }

        if (mHostName != null)
            setHostname(mHostName);

        //add default webapp
        defaultContext = addWebapp(getHost(), "", contextFile.getAbsolutePath());
        if (parentClassLoader != null)
            defaultContext.setLoader(new WebappLoader(parentClassLoader));

        //http connector
        connector = createConnector(mHostName, port);
        service.addConnector(connector);

        if(tomcatConfigs != null && !tomcatConfigs.isEmpty()) {
            final Iterator<String> it = tomcatConfigs.keySet().iterator();
            while (it.hasNext()) {
                final String propertyName = it.next();
                final Object propertyValue = tomcatConfigs.get(propertyName);
                if (propertyValue != null) {
                    connector.setAttribute(propertyName, propertyValue);
                }
            }
        }

        if (sslConfig != null && sslConfig.enableSSL) {
            // additional SSL connector
            Connector sslConnector = createConnector(mHostName, sslConfig.sslPort);
            setupSSL(sslConnector, sslConfig);
            service.addConnector(sslConnector);

            LOGGER.info("Created SSL connector on port: " + sslConfig.sslPort);
            connector.setRedirectPort(sslConfig.sslPort);
        }

        super.init();
    }

    private void                initCatalinaProperties() {
        Properties properties = new Properties();
        File props = getPropertiesFile(CATALINA_PROPERTIES_FILE);
        try (InputStream is = new FileInputStream(props)) {
            properties.load(is);
        } catch (Exception e) {
            LOGGER.warning("Can't load catalina.properties file: " + e.getMessage());
            properties = null;
        }

        if (properties != null) {
            //Register the properties as system properties
            Enumeration<?> enumeration = properties.propertyNames();
            while (enumeration.hasMoreElements()) {
                String name = (String) enumeration.nextElement();
                String value = properties.getProperty(name);
                if (value != null) {
                    System.setProperty(name, value);
                }
            }
        } else {
            //set some options programmatically
            System.setProperty(org.apache.tomcat.util.scan.Constants.SKIP_JARS_PROPERTY, "*");
            System.setProperty("org.apache.el.parser.SKIP_IDENTIFIER_CHECK", "true");
            System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");
            System.setProperty("org.apache.coyote.USE_CUSTOM_STATUS_MSG_IN_HEADER", "true");
        }
    }

    private File getPropertiesFile(String fileName) {
        File propFile = QSHome.getFile(fileName);
        if (!propFile.exists()) {
            extractResource(fileName, propFile);
        }

        return propFile;
    }

    private void extractResource(String fileName, File outputFile) {
        try {
            outputFile.getParentFile().mkdirs();
            IOUtil.extractResource(fileName, outputFile);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Can't extract resource '" + fileName + "'", e);
        }
    }

    private void                setupSSL(Connector connector, SSLProperties props) {
        // setup ssl attributes
        connector.setAttribute("SSLEnabled", "true");
        connector.setAttribute("scheme", "https");
        connector.setAttribute("secure", "true");
        connector.setAttribute("clientAuth", "want");
        connector.setAttribute("keystoreFile", props.keystoreFile);
        connector.setAttribute("keystorePass", props.keystorePass);
    }

    private Connector           createConnector(String host, int port) {
        DXConnector connector = new DXConnector();

        if (host != null)
            connector.setAttribute ("address", host);

        connector.setAttribute ("compression", "on");
        connector.setAttribute ("compressableMimeType", "text/xml,application/deltix-quantserver");

        connector.setPort(port);

        connector.setConnectionHandler(connectionHandler);

        return connector;
    }

    public void                 setSSLConfig(SSLProperties config) {
        this.sslConfig = config;
    }

    public SSLProperties        getSSLConfig() {
        return sslConfig;
    }

    public Context              addModule (String webApp) {
        return addModule (webApp, Home.getFile ("web/" + webApp));
    }

    public void                 addHelpSite (String webApp, String bookName) {
        addModule (webApp, Home.getFile ("docs/guide/html/" + bookName));
    }

    public Context              addModule (String webApp, File docBase) {
        if ( ! docBase.exists())
            LOGGER.severe("Web app doesn't exist: " + docBase);

        Context context = null;
        try {
            context = addWebapp("/" + webApp, docBase.getAbsolutePath());
            if (parentClassLoader != null)
                context.setLoader(new WebappLoader(parentClassLoader));
        } catch (ServletException e) {
            e.printStackTrace();
        }

        configureContext(context);
        return context;
    }

    /**
     * This method was copied from base Tomcat class and extended to load conf/web.xml.
     */
    @Override
    public Context addWebapp(Host host, String url, String path) {
        //silence(host, url);
        StandardContext ctx = new StandardContext();
        ctx.setPath(url);
        ctx.setDocBase(path);
        ctx.setConfigFile(getWebappConfigFile(path, url));
        ctx.setUnpackWAR(false);

        // By default, Tomcat tries to fix a memory leak in Java implementation:
        // https://bugs.openjdk.org/browse/JDK-8277072
        // However Tomcat's implementation (Tomcat 8.0.x) is not compatible with Java 11+.
        // Also, this clean up it not really needed for TimeBase because we don't reload web applications.
        // So we disable this feature to avoid unnecessary warning messages in the log.
        ctx.setClearReferencesObjectStreamClassCaches(false);

        ContextConfig ctxCfg = new ContextConfig();
        ctx.addLifecycleListener(ctxCfg);

        File confWebXml = getPropertiesFile(WEB_PROPERTIES_FILE);
        if (confWebXml.exists() && confWebXml.isFile()) {
            ctxCfg.setDefaultWebXml(confWebXml.getAbsolutePath());
        } else {
            //load defaults programmatically
            ctxCfg.setDefaultWebXml(Constants.NoDefaultWebXml);
            ctx.addLifecycleListener(new DefaultWebXmlListener());
        }

        if (host == null) {
            getHost().addChild(ctx);
        } else {
            host.addChild(ctx);
        }

        return ctx;
    }

    public static void          addServlet(Context context, String name, String mapping, Class<? extends Servlet> cls) {
        Wrapper newWrapper = context.createWrapper();
        newWrapper.setName(name);
        newWrapper.setLoadOnStartup(1);
        newWrapper.setServletClass(cls.getName());
        context.addChild(newWrapper);
        context.addServletMapping(mapping, newWrapper.getName());
    }

    public void                 setWorkDir (File f) {
        workDir = f;
    }

    private String              getTomcatWorkDir() {
        if (workDir == null)
            workDir = QSHome.getFile("work/tomcat");

        workDir.mkdirs();
        if ( ! workDir.exists())
            throw new com.epam.deltix.util.io.UncheckedIOException("Error creating Tomcat work directory \"" + workDir.getAbsolutePath()+'"');

        return workDir.getAbsolutePath();
    }

    public void             setHostStr (String name) throws UnknownHostException {
        if (name != null)
            mHostName = name;
    }

    public void             setPortStr (String port) throws NumberFormatException {
        if (port != null)
            setPort(Integer.parseInt (port));
    }

    @Override
    public void             stop() throws LifecycleException {
        super.stop();
        stopSignal.set();
    }

    /**
     * Blocking version of start() method.
     */
    public void             startWithWait() throws LifecycleException {
        super.start();
        waitForStop();
    }

    /**
     * Waits DXTomcat to stop. Use this method, when you call start() to start DXTomcat.
     */
    public void             waitForStop() {
        try {
            stopSignal.await();
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Awaiting has been interrupted.", e);
        }
    }

    public class DXConnector extends Connector {
        public DXConnector() {
            super(DX_CONNECTOR_CLASSNAME);
        }

        public void setConnectionHandler(ConnectionHandler handler) {
            ((Http11DXProtocol) protocolHandler).setConnectionHandler(handler);
        }
    }

}