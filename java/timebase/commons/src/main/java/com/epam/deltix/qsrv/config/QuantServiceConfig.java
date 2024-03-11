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
package com.epam.deltix.qsrv.config;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.SSLProperties;
import com.epam.deltix.qsrv.util.text.Mangle;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.TokenReplacingReader;
import com.epam.deltix.util.lang.Depends;
import com.epam.deltix.util.lang.DependsOnClass;
import com.epam.deltix.util.lang.SortedProperties;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.os.CommonSysProps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
/**
 *  After setting up QSHome, this provides a small generic framework
 *  for determining the host, port and other properties of a service.
 */
@Depends({
        "config/logger.properties",
    "config/gflog.xml"
})

public class QuantServiceConfig {
    private static final Log LOG = LogFactory.getLog(QuantServiceConfig.class);

    public static final String HOST_PROP                   = "host";
    public static final String PORT_PROP                   = "port";
    public static final String WEB_PORT_PROP               = "webPort";
    public static final String TB_LOGIN_USER               = "security.tbLogin.user";
    public static final String TB_LOGIN_PASS               = "security.tbLogin.password";
    public static final String ENABLE_SSL                  = "enableSSL";
    public static final String SSL_PORT                    = "sslInfo.sslPort";
    public static final String SSL_FOR_LOOPBACK            = "sslInfo.sslForLoopback";
    public static final String SSL_KEYSTORE_FILE_PROPNAME  = "sslInfo.keystoreFile";
    public static final String SSL_KEYSTORE_PASS_PROPNAME  = "sslInfo.keystorePass";
    public static final String SERVICE_FAILURE_ACTION      = "serviceFailureAction";
    public static final String SERVICE_RESTART_DELAY       = "serviceFailureAction.restartDelay";
    public static final String SERVICE_RESTART_ATTEMPTS    = "serviceFailureAction.restartAttempts";

    public static final String ADMIN_PROPS                 = "config/admin.properties";
    public static final String TICK_DB_FOLDER              = "timebase";
    public static final String OLD_TICK_DB_FOLDER          = "tickdb";

    public static final String  QSRV_TYPE_SYS_PROP         = "deltix.qsrv.type";

    public static final String  ENABLE_REMOTE_ACCESS       = "enableRemoteAccess";

    public static final String WEBAPP_PATH                 = "webapp.path";

    public static final String ENABLE_METRICS              = "enableMetrics";
    public static final String DISABLE_JVM_METRICS          = "metricsService.disableJvmMetrics";

    private static final String ENABLE_SSL_SYS_PROP        = "TimeBase.enableSSL";

    public enum Type {
        TimeBase,
        UHF,
        Aggregator,
        QuantServer,
        ExecutionServer,
        StrategyServer
    }

    private final Type              myType;
    private final Properties        props;

    private QuantServiceConfig (Type type) throws IOException {
        this.myType = type;
        //
        //  Load admin props
        //
        props = new SortedProperties ();
        loadProperties(QSHome.getFile (ADMIN_PROPS), props);

        //sslProperties = new SSLProperties();
    }

    private QuantServiceConfig (Type type, Properties propertySource) {
        this.myType = type;
        props = new SortedProperties ();
        props.putAll(propertySource);
    }

    public static QuantServiceConfig    forService (Type type)
            throws IOException
    {
        CommonSysProps.mergePropsOnceIfFileExists();

        return (new QuantServiceConfig (type));
    }

    public static QuantServiceConfig    forApp (DefaultApplication app, Type type)
            throws IOException
    {
        CommonSysProps.mergePropsOnceIfFileExists();

        QSHome.set (app.getArgValue ("-home"));

        QuantServiceConfig      config = new QuantServiceConfig (type);

        config.setStringFromCmdLine (app, "-host", HOST_PROP, "localhost");
        config.setStringFromCmdLine (app, "-port", PORT_PROP, null);

        return (config);
    }

    /**
     * Creates {@link QuantServiceConfig} with configuration populated from provided {@link Properties} object.<p>
     *
     * This method is supposed to be used from tests where you may want to construct {@link QuantServiceConfig}
     * programmatically (instead of reading it from file).<p>
     *
     * Note that this method does perform token replacement
     *
     * @param type type of service
     * @param propertySource source of configuration (will not be modified)
     * @return configuration
     */
    public static QuantServiceConfig    fromExternalProperties (Type type, Properties propertySource)
    {
        return new QuantServiceConfig (type, propertySource);
    }

    public Type                         getType() {
        return myType;
    }

    public void                         setStringFromCmdLine (
            DefaultApplication                  app,
            String                              arg,
            String                              key,
            Object                              defaultValue
    )
            throws IOException
    {
        setStringFromCmdLine(app, arg, myType, key, defaultValue);
    }

    public void                         setStringFromCmdLine (
            DefaultApplication                  app,
            String                              arg,
            Type                                type,
            String                              key,
            Object                              defaultValue
    )
            throws IOException
    {
        setPropertySoft (type, key, defaultValue);
        setProperty(type, key, app.getArgValue(arg));
    }

    public void                 setProperty (String key, Object value) {
        setProperty(myType, key, value);
    }

    public void                 clearProperty (String key) {
        clearProperty(myType, key);
    }

    /**
     *  Set property only if it is not set yet.
     */
    public void                 setPropertySoft (String key, Object value) {
        setPropertySoft (myType, key, value);
    }

    public String               getString (String key) {
        return (getString(key, null));
    }

    public String               getString (String key, String defaultValue) {
        return (getString(myType, key, defaultValue));
    }

    public int                  getInt (String key, int defaultValue) {
        return (getInt(myType, key, defaultValue));
    }

    public long                 getLong(String key, long defaultValue) {
        return (getLong(myType, key, defaultValue));
    }

    public boolean              getBoolean(String key, boolean defaultValue) {
        return (getBoolean(myType, key, defaultValue));
    }

    public String               getHost () {
        return (getHost(myType));
    }

    public String               getHostOrGuess () {
        return (getHostOrGuess(myType));
    }

    public int                  getPort () {
        return (getPort (0));
    }

    public int                  getPort (int defPort) {
        return (getPort (myType, defPort));
    }

    public int                  getWebPort (int defPort) {
        return (getWebPort (myType, defPort));
    }

    public void                 setPort (int port) {
        setProperty (myType, PORT_PROP, port);
    }

    public void                 setWebPort(int port) {
        setProperty (myType, WEB_PORT_PROP, port);
    }

    public String               getTBLogin(){
        return getString(Type.QuantServer, TB_LOGIN_USER, null);
    }

    public String               getTBPass (){
        return Mangle.split(getString(Type.QuantServer, TB_LOGIN_PASS, null));
    }

//    public void                 setUser (String user){
//        setProperty (Type.QuantServer, TB_LOGIN_USER, user);
//    }
//
//    public void                 setPass (String pass){
//        setProperty (Type.QuantServer, TB_LOGIN_PASS, concat(pass));
//    }

    public SSLProperties getSSLConfig() {
        boolean enabled = getBoolean(ENABLE_SSL, false);

        if (enabled || Boolean.getBoolean(ENABLE_SSL_SYS_PROP)) {
            SSLProperties ssl = new SSLProperties(true);

            ssl.sslPort = getInt(SSL_PORT, getSSLPort(getPort()));
            ssl.sslForLoopback = getBoolean(SSL_FOR_LOOPBACK, false);
            ssl.keystoreFile = getString(SSL_KEYSTORE_FILE_PROPNAME, ssl.keystoreFile);
            ssl.keystorePass = Mangle.split(getString(SSL_KEYSTORE_PASS_PROPNAME, ssl.keystorePass));

            return ssl;
        }

        return null;
    }

    // TODO: MODULARIZATION
    static int                   getSSLPort(int port) {
        if (port == 0) {
            return 8022;
        }

        int sum = 0;
        int n = port;
        while (n > 0) {
            sum += n % 10;
            n /= 10;
        }
        return port + sum + 1;
    }

    public void                 setSSLConfig(SSLProperties sslProperties) {
        setProperty(ENABLE_SSL, sslProperties.enableSSL ? "True" : "False");
        setProperty(SSL_PORT, sslProperties.sslPort);
        setProperty(SSL_FOR_LOOPBACK, sslProperties.sslForLoopback ? "True" : "False");
        setProperty(SSL_KEYSTORE_FILE_PROPNAME, sslProperties.keystoreFile);
        setProperty(SSL_KEYSTORE_PASS_PROPNAME, Mangle.concat(sslProperties.keystorePass));
    }

    public boolean              useWatchdog() {
        String failureAction = getString(SERVICE_FAILURE_ACTION);
        if (failureAction != null && failureAction.compareToIgnoreCase("RESTART") == 0)
            return true;
        return false;
    }

    public int                  getServiceRestartDelay() {
        return getInt(SERVICE_RESTART_DELAY, -1);
    }

    public int                  getServiceRestartAttempts() {
        return getInt(SERVICE_RESTART_ATTEMPTS, -1);
    }

    private String              prefix (Type type) {
        return (type == null ? "" : type.name () + ".");
    }

    public void                 setProperty (Type type, String key, Object value) {
        if (value != null)
            props.setProperty (prefix (type) + key, value.toString ());
    }

    public void                 clearProperty (Type type, String key) {
        props.remove(prefix (type) + key);
    }

    public void                 setPropertySoft (Type type, String key, Object value) {
        if (value != null) {
            String      name = prefix (type) + key;

            if (!props.containsKey (name))
                props.setProperty (name, value.toString ());
        }
    }

    /** @return property bu full key */
    public String               getExactProperty (String key) {
        return props.getProperty(key);
    }

    public String               getString (Type type, String key, String defaultValue) {
        return StringUtils.trim((props.getProperty (prefix (type) + key, defaultValue)));
    }

    public int                  getInt (Type type, String key, int defaultValue) {
        String  s = getString (type, key, String.valueOf (defaultValue));

        return (s == null ? defaultValue : Integer.parseInt (s));
    }

    public boolean              getBoolean (Type type, String key, boolean defaultValue) {
        String  s = getString (type, key, String.valueOf (defaultValue));

        return (s == null ? defaultValue : Boolean.parseBoolean (s));
    }

    public long                 getLong (Type type, String key, long defaultValue) {
        String  s = getString (type, key, String.valueOf (defaultValue));

        return (s == null ? defaultValue : Long.parseLong(s));
    }

    public String               getHost (Type type) {
        boolean remote = getBoolean(type, "remote", false);
        return remote ? getString (type, HOST_PROP, null) : "localhost";
    }

    public String               getHostOrGuess (Type type) {
        final String host = getHost (type);
        return (host != null ? host : getHostname());
    }

    public void                 setHost (String host) {
        setProperty (myType, HOST_PROP, host);
    }

    public int                  getPort (Type type, int defPort) {
        return (getInt (type, PORT_PROP, defPort));
    }

    public int                  getWebPort (Type type, int defPort) {
        return (getInt (type, WEB_PORT_PROP, defPort));
    }

    public Properties           getProps () {
        return props;
    }

    private static String       getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new RuntimeException ("Can't establish local host name", e);
        }
    }

    /** @return value of boolean property with given key (false if undefined) */
    public boolean              getFlag(String key) {
        return Boolean.valueOf(getString (key, "false"));
    }

//    public String               getTBConnectionUrl() {
//        Type timebase = Type.TimeBase;
//
//        return NetUtils.INSTANCE.formatUrl(
//                TDBProtocol.getProtocol(getBoolean(timebase, ENABLE_SSL, false)),
//                getHostOrGuess(timebase),
//                getPort(timebase, TDBProtocol.DEFAULT_PORT),
//                null,
//                getTBLoginUser(),
//                Mangle.split(getTBLoginPassword())
//        );
//    }

    public String getTBLoginUser() {
        final String myServiceUser = getString(myType, TB_LOGIN_USER, null);
        return (myServiceUser != null) ? myServiceUser : getString(Type.QuantServer, TB_LOGIN_USER, null);
    }

    public String getTBLoginPassword() {
        final String myServiceUser = getString(myType, TB_LOGIN_USER, null);
        return (myServiceUser != null) ? getString(myType, TB_LOGIN_PASS, null) : getString(Type.QuantServer, TB_LOGIN_PASS, null);
    }

    private static final class PropertiesTokenReplacer implements TokenReplacingReader.TokenResolver {
        private String qsHome = null;
        private String home = null;

        @Override
        public String resolveToken(String token) {
            if (token.equalsIgnoreCase("home"))
                return getQSHome();
            if (token.equalsIgnoreCase("deltix_home"))
                return getHome();

            return System.getenv(token);
        }

        private String getQSHome() {
            if (qsHome == null) {
                qsHome = QSHome.get().replace ('\\', '/');
            }

            return qsHome;
        }

        private String getHome() {
            if (home == null) {
                home = Home.get().replace('\\', '/');
            }

            return home;
        }
    }

    /** Load admin.properties from given file, replace standard tokens */
    public static void loadProperties (File file, Properties props) throws IOException {
        try (TokenReplacingReader reader = new TokenReplacingReader(new FileReader(file), new PropertiesTokenReplacer())) {
            props.load (reader);
        } catch (FileNotFoundException x) {
            LOG.trace ("File %s is not present.").with(file.getAbsolutePath());
        }
    }

}