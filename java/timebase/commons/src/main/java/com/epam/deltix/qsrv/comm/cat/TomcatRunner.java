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
package com.epam.deltix.qsrv.comm.cat;

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

    // UHF and derived web apps use the same webapp name (which allows RPC/HTTP clients to use single dispatch servlet path)
    public static final String  UHF_WEB_APP_NAME          = "uhf";

    private DXTomcat                    mCat;
    private final StartConfiguration    config;
    private final ObjectArrayList<ServiceExecutor> executors = new ObjectArrayList<ServiceExecutor>();

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

        QuantServerExecutor executor = (QuantServerExecutor) config.getExecutor(Type.QuantServer);
        executor.run(config.quantServer);
        executors.add(0, executor);

        mCat.setConnectionHandler(QuantServerExecutor.HANDLER);

        if (config.tb != null) {
            ServiceExecutor tb = config.getExecutor(Type.TimeBase);
            tb.run(config.tb);
            executors.add(0, tb);
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

        mCat.setTomcatConfigs(TomcatConfig.getTomcatConfig(config));
        mCat.init ();

        Context tbCtx = null;
        if (config.tb != null) {
            tbCtx = mCat.addModule(TIME_BASE_WEB_APP_NAME, getWebappFile(config.tb, Home.getFile ("web/" + TIME_BASE_WEB_APP_NAME)));
        }
        boolean useSSL = isSSLEnabled();


        boolean tbUAC = QuantServerExecutor.SC != null;

        if (QuantServerExecutor.SC != null) {
            AuthenticationRealm realm = new AuthenticationRealm(QuantServerExecutor.SC);

            if (tbCtx != null) {
                tbCtx.setRealm(realm);
                tbCtx.setLoginConfig(realm.getLoginConfig());
                tbCtx.addConstraint(AuthenticationRealm.createSecurityConstraint(useSSL, true));
            }

            mCat.getDefaultContext().setRealm(realm);
            mCat.getDefaultContext().setLoginConfig(realm.getLoginConfig());
            mCat.getDefaultContext().addConstraint(AuthenticationRealm.createSecurityConstraint(false, true));
        }

        if (useSSL && tbCtx != null)
            tbCtx.addConstraint(AuthenticationRealm.createSecurityConstraint(true, tbUAC, "*.jsp"));

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