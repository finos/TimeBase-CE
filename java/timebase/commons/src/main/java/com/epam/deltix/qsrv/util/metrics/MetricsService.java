package com.epam.deltix.qsrv.util.metrics;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.time.TimeKeeper;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.binder.tomcat.TomcatMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;

import java.util.function.ToDoubleFunction;

import static java.util.Collections.emptyList;

public class MetricsService implements Disposable {

    // the property was left for backward compatibility
    private static final String ENABLE_TIMEBASE_METRICS_PROP = "TimeBase.metrics.enable";
    public static final boolean ENABLE_TIMEBASE_METRICS = Boolean.getBoolean(ENABLE_TIMEBASE_METRICS_PROP);

    private static final String ENABLE_JVM_TIMEBASE_METRICS_PROP = "TimeBase.metrics.enableJvmMetrics";
    public static final boolean ENABLE_JVM_TIMEBASE_METRICS = Boolean.getBoolean(ENABLE_JVM_TIMEBASE_METRICS_PROP);
    private static final String ENABLE_TOMCAT_TIMEBASE_METRICS_PROP = "TimeBase.metrics.enableTomcatMetrics";
    public static final boolean ENABLE_TOMCAT_TIMEBASE_METRICS = Boolean.getBoolean(ENABLE_TOMCAT_TIMEBASE_METRICS_PROP);

    private static final MetricsService INSTANCE = new MetricsService();

    public static MetricsService getInstance() {
        return INSTANCE;
    }

    public synchronized static void init(String host, int port, boolean enableJvmMetrics, boolean enableTomcatMetrics) {
        MetricsService metrics = getInstance();
        if (metrics.registry == null) {
            metrics.registry = new PrometheusMeterRegistry(
                PrometheusConfig.DEFAULT, CollectorRegistry.defaultRegistry,
                new Clock() {
                    @Override
                    public long wallTime() {
                        return TimeKeeper.currentTime;
                    }

                    @Override
                    public long monotonicTime() {
                        return System.nanoTime();
                    }
                }
            );

            metrics.registry.config().commonTags("host", host, "port", String.valueOf(port));

            if (enableJvmMetrics) {
                new ClassLoaderMetrics().bindTo(metrics.registry);
                new JvmMemoryMetrics().bindTo(metrics.registry);
                new ProcessorMetrics().bindTo(metrics.registry);
                new JvmThreadMetrics().bindTo(metrics.registry);
                new UptimeMetrics().bindTo(metrics.registry);

                metrics.jvmGcMetrics = new JvmGcMetrics();
                metrics.jvmGcMetrics.bindTo(metrics.registry);
            }

            if (enableTomcatMetrics) {
                metrics.tomcatMetrics = new TomcatMetrics(null, emptyList());
                metrics.tomcatMetrics.bindTo(metrics.registry);
            }
        }
    }

    private final static Log LOGGER = LogFactory.getLog(MetricsService.class);

    private volatile PrometheusMeterRegistry registry;
    private volatile JvmGcMetrics jvmGcMetrics;
    private volatile TomcatMetrics tomcatMetrics;

    private MetricsService() {
    }

    public boolean initialized() {
        return registry != null;
    }

    public <T extends Number> T registerGauge(String name, T number) {
        if (initialized()) {
            return registry.gauge(name, number);
        }

        return number;
    }

    public <T> T registerGauge(String name, T object, ToDoubleFunction<T> function) {
        if (initialized()) {
            return registry.gauge(name, object, function);
        }

        return object;
    }

    public String scrape() {
        checkIsNotConfigured();
        return registry.scrape(TextFormat.CONTENT_TYPE_OPENMETRICS_100);
    }

    private void checkIsNotConfigured() {
        if (!initialized()) {
            throw new IllegalStateException("Metrics Service is not configured.");
        }
    }

    @Override
    public synchronized void close() {
        closeJvmMetrics();
        closeTomcatMetrics();
        closeMetricsRegistry();
    }

    private void closeJvmMetrics() {
        try {
            if (jvmGcMetrics != null) {
                jvmGcMetrics.close();
            }
        } catch (Throwable t) {
            LOGGER.error().append("Failed to close JvmGcMetrics").append(t).commit();
        } finally {
            jvmGcMetrics = null;
        }
    }

    private void closeTomcatMetrics() {
        try {
            if (tomcatMetrics != null) {
                tomcatMetrics.close();
            }
        } catch (Throwable t) {
            LOGGER.error().append("Failed to close JvmGcMetrics").append(t).commit();
        } finally {
            tomcatMetrics = null;
        }
    }

    private void closeMetricsRegistry() {
        try {
            if (registry != null) {
                registry.close();
            }
        } catch (Throwable t) {
            LOGGER.error().append("Failed to close PrometheusMeterRegistry").append(t).commit();
        } finally {
            registry = null;
        }
    }
}
