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

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.config.QuantServerExecutor;
import com.epam.deltix.qsrv.config.QuantServiceConfig;
import com.epam.deltix.qsrv.config.QuantServiceConfig.Type;
import com.epam.deltix.qsrv.config.ServiceExecutor;
import com.epam.deltix.qsrv.util.servlet.AccessFilter;
import com.epam.deltix.snmp.QuantServerSnmpObjectContainer;
import com.epam.deltix.snmp.SNMPTransportFactory;
import com.epam.deltix.qsrv.util.tomcat.DXTomcat;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.tomcat.ConnectionHandshakeHandler;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.epam.deltix.qsrv.config.QuantServiceConfig.ENABLE_REMOTE_ACCESS;

public final class TomcatRunner {
    protected static final Logger LOGGER = Logger.getLogger (TomcatRunner.class.getName());

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

    private DXTomcat                    mCat;
    private final StartConfiguration    config;
    private final ObjectArrayList<ServiceExecutor> executors = new ObjectArrayList<ServiceExecutor>();
    
    //private BaseSpringContext           commonContext;

    public TomcatRunner(StartConfiguration config) {
        this.config = config;
    }

    public StartConfiguration getConfig() {
        return config;
    }

//    private ApplicationContext getCommonContext() throws IOException {
//        if (commonContext != null)
//            return commonContext.getSpringContext();
//
//        Properties properties = new Properties();
//        Collection<String> resources = new LinkedHashSet<>(5);
//
//        if (config.agg != null) {
//            properties.putAll(config.agg.getProps());
//        }
//
//        if (config.es != null) {
//            properties.putAll(config.es.getProps());
//            resources.add(UHF_TRADE_CTX);
//        }
//
//        if (config.sts != null) {
//            properties.putAll(config.sts.getProps());
//        }
//
//        if (config.uhf != null) {
//            properties.putAll(config.uhf.getProps()); // UHF properties have priority
//            resources.add(UHF_TRADE_CTX);
//        }
//
//        commonContext = new BaseSpringContext(null, "QuantServer", TomcatRunner.class, QSHome.getFile(),
//                                              properties, false, resources.toArray(new String[resources.size()]));
//        return commonContext.getSpringContext();
//    }

    private void             setSSLConfig(QuantServiceConfig qsConfig) {
        if (qsConfig != null)
            mCat.setSSLConfig(qsConfig.getSSLConfig());
    }

    public boolean          isSSLEnabled() {
        return mCat.getSSLConfig() != null && mCat.getSSLConfig().enableSSL;
    }

    public void             init() throws Exception {
        setRemoteAccess();

        if (mCat != null)
            throw new IllegalStateException("Tomcat already initialized");

        mCat = new DXTomcat(DEFAULT_WEB_APP_DIR, getWebappFile(config.quantServer, null), DEFAULT_WEB_APP_NAME);
        mCat.setPort(config.port);

        if (config.tb == null) {
            // UHF and derived web-applications are deployed as 'uhf' web app name.
            // Unless we set a different work folder, they will use the same work dir to cache some compiled classes for JSPs
            if (config.uhf != null)
                mCat.setWorkDir(QSHome.getFile("work/tomcat/" + UHF_WEB_APP_DIR));
            else  if (config.sts != null)
                mCat.setWorkDir(QSHome.getFile("work/tomcat/" + STS_WEB_APP_DIR));
            else if (config.es != null)
                mCat.setWorkDir(QSHome.getFile("work/tomcat/" + ES_WEB_APP_DIR));
        }

        QuantServerExecutor executor = (QuantServerExecutor) config.getExecutor(Type.QuantServer);
        executor.run(config.quantServer);
        executors.add(0, executor);

        mCat.setConnectionHandler(QuantServerExecutor.HANDLER);

        if (config.tb != null) {
            ServiceExecutor tb = config.getExecutor(Type.TimeBase);
            tb.run(config.tb);
            executors.add(0, tb);
        }

        if (config.agg != null) {
            ServiceExecutor agg = config.getExecutor(Type.Aggregator);
            agg.run(config.agg);
            executors.add(0, agg);

//            Aggregator.doRun(getCommonContext(), config.agg);
        }

        if (config.es != null) {
            ServiceExecutor es = config.getExecutor(Type.ExecutionServer);
            es.run(config.es, config.agg);
            executors.add(0, es);
            //ExecutionServerStarter.doRun(getCommonContext(), config.es, config.agg);
        }

        if (config.quantServer.getFlag("SNMP")) {
            QuantServerSnmpObjectContainer snmpObjectContainer = new QuantServerSnmpObjectContainer();
            for (ServiceExecutor serviceExecutor : executors) {
                serviceExecutor.registerSnmpObjects(snmpObjectContainer);
            }

            ConnectionHandshakeHandler connectionHandshakeHandler = SNMPTransportFactory.initializeSNMP(config.port, snmpObjectContainer);
            QuantServerExecutor.HANDLER.addHandler(
                    (byte)48, // BER.SEQUENCE
                    connectionHandshakeHandler);
        }

        //set SSL config
        if (config.tb != null)
            setSSLConfig(config.tb);

        if (config.uhf != null)
            setSSLConfig(config.uhf);

        if (config.es != null)
            setSSLConfig(config.es);

        mCat.setTomcatConfigs(TomcatConfig.getTomcatConfig(config));
        mCat.init ();

        Context tbCtx = null;
        if (config.tb != null) {
            tbCtx = mCat.addModule(TIME_BASE_WEB_APP_NAME, getWebappFile(config.tb, Home.getFile ("web/" + TIME_BASE_WEB_APP_NAME)));
        }
        boolean useSSL = isSSLEnabled();

        if (config.agg != null)
            mCat.addModule(AGGREGATOR_WEB_APP_NAME, getWebappFile(config.agg, Home.getFile("web/" + AGGREGATOR_WEB_APP_NAME))); //, Home.getFile("web/" + AGGREGATOR_WEB_APP_NAME));

        Context uhfCtx = null;
        if (config.uhf != null)
            uhfCtx = mCat.addModule(UHF_WEB_APP_NAME, Home.getFile("web/" + UHF_WEB_APP_DIR));

        Context esCtx = null;
        if (config.es != null) {
            esCtx = mCat.addModule(ES_WEB_APP_NAME, Home.getFile("web/" + ES_WEB_APP_DIR));
            config.getExecutor(Type.ExecutionServer).configure(esCtx);
        }

        if (config.sts != null)
            mCat.addModule(STS_WEB_APP_NAME, Home.getFile("web/" + STS_WEB_APP_DIR));

        boolean tbUAC = QuantServerExecutor.SC != null;

        if (QuantServerExecutor.SC != null) {
            AuthenticationRealm realm = new AuthenticationRealm(QuantServerExecutor.SC);

            if (tbCtx != null) {
                tbCtx.setRealm(realm);
                tbCtx.setLoginConfig(realm.getLoginConfig());
                tbCtx.addConstraint(AuthenticationRealm.createSecurityConstraint(useSSL, true));
            }

            if (uhfCtx != null) {
                uhfCtx.setRealm(realm);
                uhfCtx.setLoginConfig(realm.getLoginConfig());
                uhfCtx.addConstraint(AuthenticationRealm.createSecurityConstraint(useSSL, true));
            }

            if (esCtx != null) {
                esCtx.setRealm(realm);
                esCtx.setLoginConfig(realm.getLoginConfig());
                esCtx.addConstraint(AuthenticationRealm.createSecurityConstraint(useSSL, true));
            }

            mCat.getDefaultContext().setRealm(realm);
            mCat.getDefaultContext().setLoginConfig(realm.getLoginConfig());
            mCat.getDefaultContext().addConstraint(AuthenticationRealm.createSecurityConstraint(false, true));
        }

        if (useSSL && tbCtx != null)
            tbCtx.addConstraint(AuthenticationRealm.createSecurityConstraint(true, tbUAC, "*.jsp"));

        // UHF and ES use SecurityConstraintConfigurator and security.xml
        if (useSSL && esCtx != null)
            esCtx.addConstraint(AuthenticationRealm.createSecurityConstraint(true, false)); // ES use filter for authentication

        if (useSSL && uhfCtx != null)
            uhfCtx.addConstraint(AuthenticationRealm.createSecurityConstraint(true, false)); // UHF use filter for authentication

        if (useSSL) // Default web app
            mCat.getDefaultContext().addConstraint(AuthenticationRealm.createSecurityConstraint(true, tbUAC, "*.jsp", "/shutdown...", "/getlogs"));
    }

    // Destroy instance of tomcat
    private  void             destroy() throws LifecycleException {
        if (mCat != null) {
            mCat.destroy();
            mCat = null;
        }
    }

    //just starting server
    private void             start() throws Exception {
        if (mCat != null)
            mCat.start();
    }

    // just stopping server
    private void             stop() throws Exception {
        if (mCat != null)
            mCat.stop();
    }

    /*
       Init and run server
     */
    public void             run() throws Exception {
        if (mCat == null)
            init();

        start();
    }

    public void             waitForStop() {
        mCat.waitForStop();
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

        // closing executors
        for (ServiceExecutor executor : executors)
            Util.close(executor);

//        if (config.es != null)
//            try {
//                ExecutionServerStarter.onShutdown();
//            } catch (Throwable e) {
//                LOGGER.log(Level.SEVERE, e.getMessage(), e);
//            }
//
//        if (config.agg != null)
//            try {
//                Aggregator.onShutdown();
//            } catch (Throwable e) {
//                LOGGER.log(Level.SEVERE, e.getMessage(), e);
//            }
//
//        Util.close(commonContext);
//
//        try {
//            GlobalQuantServer.onShutdown ();
//            destroy();
//        } catch (Throwable e) {
//            LOGGER.log (Level.SEVERE, e.getMessage (), e);
//        }

        try {
            destroy();
        } catch (Throwable e) {
            LOGGER.log (Level.SEVERE, e.getMessage (), e);
        }
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

    public boolean          isWatchdogUsed() {
        QuantServiceConfig qsConfig = getQSConfig();
        if (qsConfig != null)
            return qsConfig.useWatchdog();
        return false;
    }

    public int              getServiceRestartDelay() {
        QuantServiceConfig qsConfig = getQSConfig();
        if (qsConfig != null)
            return qsConfig.getServiceRestartDelay();
        return -1;
    }

    public int              getServiceRestartAttempts() {
        QuantServiceConfig qsConfig = getQSConfig();
        if (qsConfig != null)
            return qsConfig.getServiceRestartAttempts();
        return -1;
    }

    private static File     getWebappFile(QuantServiceConfig config, File def) {
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
