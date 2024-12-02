package com.epam.deltix.util.time;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.pub.TimeSource;

import javax.annotation.Nullable;

public class DefaultTimeSourceProvider {
    private static final Log LOG = LogFactory.getLog(DefaultTimeSourceProvider.class);
    public static final String TIME_SOURCE_SYS_PROP = "deltix.util.time.DefaultTimeSourceProvider.clock";
    private static volatile TimeSource configuredInstance;

    private DefaultTimeSourceProvider() {
    }

    public static TimeSource getTimeSourceForApp(String appName) {
        if (configuredInstance == null) {
            Class var1 = DefaultTimeSourceProvider.class;
            synchronized(DefaultTimeSourceProvider.class) {
                if (configuredInstance == null) {
                    String sysProperty = System.getProperty("deltix.util.time.DefaultTimeSourceProvider.clock");
                    if (sysProperty == null) {
                        configuredInstance = getDefaultFallback();
                        LOG.info("Selected time source: %s (default, implicitly configured by %s)").with(configuredInstance.getClass().getSimpleName()).with(appName);
                    } else {
                        configuredInstance = getTimeSourceByNameWithFallback(sysProperty);
                        LOG.info("Selected time source: %s (implicitly configured by %s)").with(configuredInstance.getClass().getSimpleName()).with(appName);
                    }
                }
            }
        }

        return configuredInstance;
    }

    public static void configure(String appName, TimeSource timeSource) {
        Class var2 = DefaultTimeSourceProvider.class;
        synchronized(DefaultTimeSourceProvider.class) {
            if (configuredInstance != null) {
                throw new IllegalStateException("Time source is already configured");
            } else {
                configuredInstance = timeSource;
                LOG.info("Selected time source: %s (explicitly configured by %s)").with(configuredInstance.getClass().getSimpleName()).with(appName);
            }
        }
    }

    private static TimeSource getDefaultFallback() {
        return KeeperTimeSource.INSTANCE;
    }

    public static @Nullable TimeSource getTimeSourceByName(String sourceName) {
        switch (sourceName) {
            case "MonotonicRealTimeSource":
                return MonotonicRealTimeSource.getInstance();
            case "KeeperTimeSource":
                return KeeperTimeSource.INSTANCE;
            default:
                return null;
        }
    }

    private static TimeSource getTimeSourceByNameWithFallback(String sysProperty) {
        TimeSource timeSourceByName = getTimeSourceByName(sysProperty);
        if (timeSourceByName == null) {
            LOG.warn().append("Unknown time source name: ").append(sysProperty).append(" (will use default)").commit();
            timeSourceByName = getDefaultFallback();
        }

        return timeSourceByName;
    }

    static void unconfigure() {
        Class var0 = DefaultTimeSourceProvider.class;
        synchronized(DefaultTimeSourceProvider.class) {
            configuredInstance = null;
        }
    }
}