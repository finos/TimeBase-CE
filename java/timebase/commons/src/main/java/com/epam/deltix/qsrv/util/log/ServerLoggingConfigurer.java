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
package com.epam.deltix.qsrv.util.log;

import com.epam.deltix.gflog.api.LogDebug;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.gflog.core.LogConfig;
import com.epam.deltix.gflog.core.LogConfigFactory;
import com.epam.deltix.gflog.core.LogConfigurator;
import com.epam.deltix.gflog.core.appender.Appender;
import com.epam.deltix.gflog.core.layout.TemplateLayoutFactory;
import com.epam.deltix.gflog.jul.JulBridge;
import com.epam.deltix.gflog.mail.appender.SmtpAppenderFactory;
import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.config.QuantServiceConfig;
import com.epam.deltix.qsrv.util.text.Mangle;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;


public class ServerLoggingConfigurer {

    private static final String TYPE_KEY = "type";
    private static final String QSHOME_KEY = "qshome";

    private static final String ALERTS_KEY = "QuantServer.alerts";
    private static final String ALERTS_SMTP = "SMTPAlerts";
    private static final String[] SMPT_ALERTS_KEYS = {
            ALERTS_KEY + ".smtpTo",
            ALERTS_KEY + ".smtpFrom",
            ALERTS_KEY + ".host",
            ALERTS_KEY + ".smtpPort",
            ALERTS_KEY + ".smtpSecure",
            ALERTS_KEY + ".smtpUsername",
            ALERTS_KEY + ".smtpPassword",
            ALERTS_KEY + ".smtpSubjPrefix"
    };

    public static void configure(QuantServiceConfig config) throws Exception {
        System.setProperty("java.util.logging.manager", "com.epam.deltix.gflog.jul.JulBridgeManager");
        GFLoggingConfigurer.configure(config, createProperties(config));
    }

    public static void unconfigure() {
        GFLoggingConfigurer.unconfigure();
    }

    private static Properties createProperties(QuantServiceConfig config) throws IOException {
        return createProperties(config.getProps(), config.getType().name());
    }

    private static Properties createProperties(Properties from, String logFileName) throws IOException {
        Properties properties = new Properties(System.getProperties());

        // put all server properties
        properties.putAll(from);
        // add
        String qshomePath = QSHome.getFile().getCanonicalPath();

        properties.put(QSHOME_KEY, qshomePath);
        properties.put(TYPE_KEY, logFileName);

        return properties;
    }

    public static Level getJULLevel(com.epam.deltix.gflog.api.LogLevel level) {
        switch (level) {
            case TRACE:
                return Level.ALL;
            case DEBUG:
                return Level.FINE;
            case INFO:
                return Level.INFO;
            case WARN:
                return Level.WARNING;
            case ERROR:
            case FATAL:
                return Level.SEVERE;
        }

        throw new IllegalArgumentException("Invalid level: " + level);
    }

    private static final class JavaUtilLoggingConfigurer {

        private static void configure(LogConfig config) throws Exception {
            final Properties properties = new Properties();
            properties.put("handlers", JulBridge.class.getName());

            ArrayList<com.epam.deltix.gflog.core.Logger> loggers = new ArrayList<>(config.getLoggers());
            loggers.sort(Comparator.comparing(com.epam.deltix.gflog.core.Logger::getName));

            for (com.epam.deltix.gflog.core.Logger gfLogger : loggers) {
                LogLevel logLevel = gfLogger.getLevel();
                String logName = gfLogger.getName();

                Level level = getJULLevel(logLevel);
                properties.setProperty(logName + ".level", level.getName());
            }

            LogManager.getLogManager().readConfiguration(toInputStream(properties)); // the only way to make it work
        }

        private static InputStream toInputStream(Properties properties) throws IOException {
            StringWriter writer = new StringWriter(1024);
            properties.store(writer, null);
            return new ByteArrayInputStream(writer.toString().getBytes());
        }

    }

    private static final class GFLoggingConfigurer {

        private static final String QS_HOME_CONFIG_OLD = QSHome.getFile("/config/gflogger.xml").getAbsoluteFile().toString();

        private static final String QS_HOME_CONFIG = QSHome.getFile("/config/gflog.xml").getAbsoluteFile().toString();
        private static final String DELTIX_HOME_CONFIG = "classpath:config/gflog.xml";

        private static LogConfig configure(QuantServiceConfig qsConfig, Properties properties) {
            warnIfGfLoggerConfigExists();

            LogConfig config = null;

            if (new File(QS_HOME_CONFIG).exists()) {
                try {
                    config = configure(QS_HOME_CONFIG, qsConfig, properties);
                } catch (final Throwable e) {
                    LogDebug.warn("Can't load the log config from QS Home: " +
                                    QS_HOME_CONFIG +
                                    ". Will use the log config from Deltix Home",
                            e);
                }
            }

            if (config == null) {
                try {
                    config = configure(DELTIX_HOME_CONFIG, qsConfig, properties);
                } catch (final Throwable e) {
                    LogDebug.warn("Can't load the log config from Deltix Home: " +
                                    DELTIX_HOME_CONFIG +
                                    ". Will use the default config",
                            e);
                }
            }

            if (config == null) {
                config = LogConfigFactory.loadDefault();
                LogConfigurator.configure(config);
            }

            return config;
        }

        private static LogConfig configure(final String url, final QuantServiceConfig qsConfig, final Properties properties) throws Exception {
            LogConfig config = LogConfigFactory.load(url, properties);
            configureSmtpAlerts(qsConfig, properties, config);

            LogConfigurator.configure(config);
            return config;
        }

        public static void unconfigure() {
            LogConfigurator.unconfigure();
        }

        private static void warnIfGfLoggerConfigExists() {
            if (new File(QS_HOME_CONFIG_OLD).exists()) {
                LogDebug.warn("The old gflogger.xml config exists but not used: " + QS_HOME_CONFIG_OLD);
            }
        }

        private static void configureSmtpAlerts(QuantServiceConfig qsConfig, Properties properties, LogConfig config) {
            final boolean smtpAlertsEnabled = ALERTS_SMTP.equals(properties.getProperty(ServerLoggingConfigurer.ALERTS_KEY));
            if (smtpAlertsEnabled) {
                SmtpAppenderFactory appenderFactory = new SmtpAppenderFactory();

                TemplateLayoutFactory layoutFactory = new TemplateLayoutFactory();
                layoutFactory.setTemplate("%d{d MMM HH:mm:ss} %p %m%n");

                appenderFactory.setLayout(layoutFactory.create());
                appenderFactory.setTo(getString(properties, ServerLoggingConfigurer.SMPT_ALERTS_KEYS[0]));
                appenderFactory.setFrom(getString(properties, ServerLoggingConfigurer.SMPT_ALERTS_KEYS[1]));
                appenderFactory.setHost(getString(properties, ServerLoggingConfigurer.SMPT_ALERTS_KEYS[2]));
                appenderFactory.setPort(getInt(properties, ServerLoggingConfigurer.SMPT_ALERTS_KEYS[3]));
                appenderFactory.setSecure(getString(properties, ServerLoggingConfigurer.SMPT_ALERTS_KEYS[4]));
                appenderFactory.setUsername(getString(properties, ServerLoggingConfigurer.SMPT_ALERTS_KEYS[5]));
                appenderFactory.setPassword(Mangle.split(getString(properties, ServerLoggingConfigurer.SMPT_ALERTS_KEYS[6])));
                appenderFactory.setSubject(getSubject(ServerLoggingConfigurer.SMPT_ALERTS_KEYS[7], qsConfig, properties));

                Appender appender = appenderFactory.create();
                config.addAppender(appender);

                for (final com.epam.deltix.gflog.core.Logger logger : config.getLoggers()) {
                    logger.addAppender(appender);
                }
            }
        }

        private static String getSubject(String key, QuantServiceConfig qsConfig, Properties properties) {
            String serverName = qsConfig.getType().name();
            String prefix = getString(properties, key, "");

            if (!prefix.isEmpty()) {
                prefix += " ";
            }

            return prefix + serverName + " [" +
                    getString(properties, serverName + "." + QuantServiceConfig.HOST_PROP) + ":" +
                    getString(properties, serverName + "." + QuantServiceConfig.PORT_PROP) +
                    "] Failure";
        }

        private static String getString(Properties properties, String key) {
            String value = properties.getProperty(key);

            if (value != null) {
                value = value.trim();

                if (value.isEmpty()) {
                    value = null;
                }
            }

            return value;
        }

        private static String getString(Properties properties, String key, String defaultValue) {
            String value = getString(properties, key);
            return value == null ? defaultValue : value;
        }

        private static int getInt(Properties properties, String key) {
            return Integer.parseInt(getString(properties, key));
        }

    }

}