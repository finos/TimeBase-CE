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
package com.epam.deltix.qsrv;

import java.lang.management.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

public class MemoryMonitorConfigurer {
    private static final Log LOG = LogFactory.getLog(MemoryMonitorConfigurer.class);
    public static final String QS_MEMORYMONITOR_PREFIX = "QuantServer.memoryMonitor";

    public static final String MEMORY_USAGE_PERCENTAGE = QS_MEMORYMONITOR_PREFIX + ".memoryUsagePercentage";
    public static final String PRINT_STACK_TRACES      = QS_MEMORYMONITOR_PREFIX + ".printStackTraces";

    public static void configure(Properties props, final long maxMemory) {
        boolean isMemoryMonitorEnabled = StringUtils.trim(props.getProperty(QS_MEMORYMONITOR_PREFIX, null)) != null;
        if (!isMemoryMonitorEnabled)
            return;

        double percentage = Double.parseDouble(props.getProperty(MEMORY_USAGE_PERCENTAGE, "80")) / 100;

        LOG.info("Start memory monitoring: threshold = %s GB").with(maxMemory * percentage / 1024 / 1024 / 1024);
        JMXMemoryMonitor memoryMonitor;
        JMXMemoryMonitor.getInstance().setPercentageUsageThreshold(percentage);
        memoryMonitor = JMXMemoryMonitor.getInstance();

        final boolean printStackTraces = Boolean.valueOf(props.getProperty(PRINT_STACK_TRACES, "false"));
        memoryMonitor.addMemoryListener((usedMemory, maxMemory1) -> LOG.error(getMemoryUsageLowMessage(usedMemory, maxMemory1, printStackTraces)));
    }

    private static String getMemoryUsageLowMessage(long usedMemory, long maxMemory, boolean printStackTraces) {
        StringBuilder result = new StringBuilder(printStackTraces ? 32 * 1024 : 512);

        result.append("Application is running low on memory!");
        result.append(String.format(" Used Memory: %,d Mb, Max Memory: %,d Mb", usedMemory / (1024 * 1024), maxMemory / (1024 * 1024)));

        if (!printStackTraces)
            return result.toString();

        Map<Thread,StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        for (Thread thread : stackTraces.keySet()) {
            result.append('\n').append(thread.toString()).append('\n');
            for (StackTraceElement traceElement : stackTraces.get(thread)) {
                result.append('\t').append(traceElement.toString()).append('\n');
            }
        }
        return result.toString();
    }

    /**
     * Tenured Space Pool can be determined by it being of type
     * HEAP and by it being possible to set the usage threshold.
     */
    private static MemoryPoolMXBean findTenuredGenPool() {
        for (MemoryPoolMXBean pool :
                ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP && pool.isUsageThresholdSupported()) {
                return pool;
            }
        }
        throw new AssertionError("Could not find tenured space");
    }

    //////////////////////////////// HELPER CLASSES /////////////////////////

    public static class JMXMemoryMonitor {
        private static final JMXMemoryMonitor INSTANCE = new JMXMemoryMonitor();

        private static final MemoryPoolMXBean tenuredGenPool = findTenuredGenPool();

        protected static final Logger LOGGER = Logger.getLogger("deltix.util.memory.monitor");
        protected static final long MEGABYTE = 1024 * 1024;

        private CopyOnWriteArrayList<MemoryListener> listeners = new CopyOnWriteArrayList<MemoryListener>();

        protected void fireMemoryUsageLow(long usedMemory, long maxMemory) {
            for (MemoryListener listener : listeners) {
                try {
                    listener.memoryUsageLow(usedMemory, maxMemory);
                } catch (Exception e) {
                    Util.handleException(e);
                }
            }
        }

        public void addMemoryListener(MemoryListener listener) {
            listeners.addIfAbsent(listener);
        }

        public void removeMemoryListener(MemoryListener listener) {
            listeners.remove(listener);
        }

        private JMXMemoryMonitor() {
            MemoryMXBean mbean = ManagementFactory.getMemoryMXBean();
            NotificationEmitter emitter = (NotificationEmitter) mbean;
            emitter.addNotificationListener(new NotificationListener() {
                public void handleNotification(Notification n, Object hb) {
                    if (n.getType().equals(MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED)) {
                        long maxMemory = tenuredGenPool.getUsage().getMax();
                        long usedMemory = tenuredGenPool.getUsage().getUsed();

                        if (LOGGER.isLoggable(Level.FINE))
                            LOGGER.log(Level.FINE, "[MemoryMonitor] memory usage low - used: {0} Mb, max: {1} Mb", new Object[] {usedMemory / MEGABYTE, maxMemory / MEGABYTE});

                        fireMemoryUsageLow(usedMemory, maxMemory);
                    }
                }
            }, null, null);
        }

        public static JMXMemoryMonitor getInstance() {
            return INSTANCE;
        }

        public void setPercentageUsageThreshold(double percentage) {
            if (percentage <= 0.0 || percentage > 1.0)
                throw new IllegalArgumentException("Percentage [" + percentage + "] not in range: [0,1)");

            long maxMemory = tenuredGenPool.getUsage().getMax();
            long warningThreshold = (long) (maxMemory * percentage);
            tenuredGenPool.setUsageThreshold(warningThreshold);
        }
    }

    public static interface MemoryListener {
        void memoryUsageLow(long usedMemory, long maxMemory);
    }

}
