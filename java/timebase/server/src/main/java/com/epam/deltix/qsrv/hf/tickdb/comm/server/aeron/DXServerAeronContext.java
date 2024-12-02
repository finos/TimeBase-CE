/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.config.QuantServiceConfig;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.multicast.AeronMulticastStreamContext;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.IdGenerator;
import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.thread.affinity.PinnedThreadFactoryWrapper;
import com.epam.deltix.util.BitUtil;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.vsocket.VSChannel;
import io.aeron.Aeron;
import io.aeron.driver.Configuration;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.exceptions.DriverTimeoutException;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.logging.Level;

/**
 * Contains all settings related to "topics" feature and Aeron lib config.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class DXServerAeronContext {

    private static final Log LOG = LogFactory.getLog(DXServerAeronContext.class);

    protected static final boolean ENABLED_BY_DEFAULT = false;

    public static final String SYS_PROP_TIME_BASE_AERON_ENABLED = "TimeBase.aeron.enabled";

    private static final String SYS_PROP_TERM_BUFFER_LENGTH = "TimeBase.transport.aeron.topic.term.buffer.length";
    private static final String SYS_PROP_TERM_BUFFER_LENGTH_OLD = "TimeBase.transport.aeron.topic.ipc.term.buffer.length";

    public static final String CONF_PROP_TOPICS_COPY_TO_STREAM_IDLE_STRATEGY = "topics.copyToStream.idleStrategy";

    /**
     * See {@link #topicTotalTermBufferLimit}
     */
    public static final String SYS_PROP_TOPIC_TERM_BUFFER_LIMIT = "TimeBase.topics.totalTermMemoryLimit";

    // Aeron driver communication timeout, in seconds
    // This timeout defines how fast we detect that driver is unavailable
    // Too low value may break service operation after GC or a pause caused by debug
    // Too high value may cause Aeron client to not detect that driver (or local timebase) is down for long time
    // If you want to debug application that uses topics you should set this value to a higher value (like 300, i.e. 5 minutes)
    public static final int DRIVER_COMMUNICATION_TIMEOUT = Integer.getInteger("TimeBase.transport.aeron.driverTimeout", 300);

    // We need 4x IPC_TERM_BUFFER_LENGTH memory per cursor and 2x IPC_TERM_BUFFER_LENGTH memory per loader.
    // So increase with caution.

    // Buffer size for Aeron-based cursors and loaders.
    private static final int IPC_TERM_BUFFER_LENGTH = Integer.getInteger("TimeBase.transport.aeron.ipc.term.buffer.length", 2 * 1024 * 1024); // 2Mb default

    private static final String DRIVER_STRATEGY = System.getProperty("TimeBase.transport.aeron.driverStrategy");

    // Address of multicast group.
    // Note: 1) different IP for different topics means independent network routing;
    //       2) same IP but different port means routing/filtering on the network/OS level.
    //       3) same IP and same mort but different streamId means routing/filtering on the app level.
    public static final String MULTICAST_ADDRESS = System.getProperty("TimeBase.transport.aeron.udp.multicast.address", "239.0.1.37:40456");

    // Multicast interface to send data to.
    public static final String MULTICAST_INTERFACE = System.getProperty("TimeBase.transport.aeron.udp.multicast.interface", null);

    // Default TTL for multicast packets.
    public static final Integer MULTICAST_TTL = Integer.getInteger("TimeBase.transport.aeron.udp.multicast.ttl", null);

    // This value is used by "Single Publisher" type Topics
    public static final int SINGLE_PUBLISHER_TOPIC_DEFAULT_PUBLISHER_PORT = Integer.getInteger("TimeBase.transport.aeron.topic.udp.single.publisher.default.port", 40491);
    public static final int SINGLE_PUBLISHER_TOPIC_DEFAULT_SUBSCRIBER_PORT = Integer.getInteger("TimeBase.transport.aeron.topic.udp.single.subscriber.default.port", 40492);

    // Sets id range to be used for Aeron stream ID. If set then TB will use only values from the specified range for Aeron Stream IDs.
    // Last generated ID will be written into a file after each new stream created.
    // This option usually makes sense only when used together with "TimeBase.transport.aeron.external.driver.dir" option.
    // In that case it's important to ensure that different applications that use same shared Aeron driver do not have overlapping Stream ID ranges.
    // Value must contain two integer numbers (lowest and highest possible values) separated by ":". Values may be negative.
    // Example value: "-100000000:299999999".
    static final String RANGE_PROP_NAME = "TimeBase.transport.aeron.id.range";
    public static final String ID_RANGE = System.getProperty(RANGE_PROP_NAME, null);

    private static final boolean DEBUG_MODE = false;

    // Buffer size for Aeron based topics (per topic).
    private final int defaultTopicTermBufferLength;

    /**
     * If total term buffer size of all topics exceeds this value, then new topics will not be created.
     *
     * <p>Please take into account that this limit uses very simplified model that assumes that
     * there no more than one producer for every topic
     *
     * <p>See <a href="https://gitlab.deltixhub.com/Deltix/QuantServer/QuantServer/-/issues/1098">Issue #1098</a>
     */
    private final long topicTotalTermBufferLimit;

    /**
     * If false, then Aeron client on the TimeBase server side should be disabled.
     *
     * <p>However, it does not mean that client can't use Aeron.
     * For instance, it is possible that two client instances will use some external Aeron driver
     * to communicate with each other. They still have to set {@link #SYS_PROP_TOPIC_TERM_BUFFER_LIMIT} property
     * on the server side to be able to create topics.
     */
    private final boolean enabled; //
    private final String aeronDir; // Directory for Aeron driver. Can be external or embedded
    private final boolean startEmbeddedDriver;

    private final String publicAddress;
    /**
     * Normally remote clients are forbidden to access IPC topics because IPC topic data are available only for
     * local processes that run on same physical machine and share a memory mapped file.
     * However, if such processes are executed in Docker containers then TimeBase may incorrectly consider them
     * as "remote" and deny access to IPC topics.
     *
     * <p>This flag disables checks for non-local clients when they access IPC topics. So local clients that have
     * non-local IP address still can access IPC.
     */
    private final boolean bypassRemoteCheckForIpcTopics;

    private State state;
    private MediaDriver driver;
    private Aeron aeron;
    private final AffinityConfig affinityConfig;

    private final Map<String, AeronMulticastStreamContext> multicastContexts = new HashMap<>();

    private static final IdGenerator aeronStreamIdGenerator = createIdGenerator();

    private final AtomicBoolean copyThreadsCanRun = new AtomicBoolean(true);

    private final IdleStrategyFactory copyToStreamIdleStrategyFactory;

    @Nonnull
    private static IdGenerator createIdGenerator() {
        if (ID_RANGE != null) {
            return FileBasedIdGenerator.createFileBasedIdGenerator(ID_RANGE);
        } else {
            if (AeronWorkDirManager.useEmbeddedDriver()) {
                // Default behavior: embedded driver and an atomic id counter
                return createStandaloneIdGenerator();
            } else {
                throw new IllegalStateException("Attempt to use external Aeron driver without providing an id range. Please use " + RANGE_PROP_NAME + " property to set range");
            }
        }
    }

    public DXServerAeronContext(boolean enabled, String aeronDir, boolean startEmbeddedDriver, @Nullable AffinityConfig affinityConfig, @Nullable String publicAddress, boolean bypassRemoteCheckForIpcTopics, IdleStrategyFactory copyToStreamIdleStrategyFactory, int topicTermBufferLength, long topicTotalTermBufferLimit) {
        if (!BitUtil.isPowerOfTwo(topicTermBufferLength)) {
            throw new IllegalArgumentException("Term buffer length must be power of 2");
        }

        this.enabled = enabled;
        this.aeronDir = aeronDir;
        this.startEmbeddedDriver = startEmbeddedDriver && enabled;
        this.affinityConfig = affinityConfig;
        this.bypassRemoteCheckForIpcTopics = bypassRemoteCheckForIpcTopics;
        this.copyToStreamIdleStrategyFactory = copyToStreamIdleStrategyFactory;
        this.state = State.NOT_STARTED;
        this.publicAddress = publicAddress;
        this.defaultTopicTermBufferLength = topicTermBufferLength;
        this.topicTotalTermBufferLimit = topicTotalTermBufferLimit;
    }

    public static DXServerAeronContext createSimple(
            boolean enabled, int tickDbPort
    ) {
        return createDefault(enabled, tickDbPort, null, null, false, null, null, null);
    }

    /**
     * @param enabled set false to disable Aeron support
     * @param tickDbPort a port number associated with current TimeBase instance. It is used to identify unique directory for Aeron driver. Does not have to be a real port.
     */
    public static DXServerAeronContext createDefault(
            boolean enabled, int tickDbPort, @Nullable AffinityConfig affinityConfig, @Nullable String publicAddress,
            boolean bypassRemoteCheckForIpcTopics, @Nullable IdleStrategyFactory copyToStreamIdleStrategyFactory,
            @Nullable Integer topicTermBufferLength, @Nullable Long topicTotalTermBufferLimit
    ) {
        String aeronDir = AeronWorkDirManager.setupWorkingDirectory(tickDbPort, System.currentTimeMillis());
        int selectedTopicTermBufferLength = determineTermBufferLength(topicTermBufferLength);

        if (copyToStreamIdleStrategyFactory == null) {
            // This does not provide low latency, but it will not eat CPU in case of many topics
            copyToStreamIdleStrategyFactory = createDefaultCopyToStreamIdleStrategy();
        }

        boolean embeddedDriver = AeronWorkDirManager.useEmbeddedDriver();

        long effectiveTopicTotalTermBufferLimit = getEffectiveTopicTotalTermBufferLimit(topicTotalTermBufferLimit, enabled, embeddedDriver);

        return new DXServerAeronContext(enabled, aeronDir, embeddedDriver, affinityConfig, publicAddress, bypassRemoteCheckForIpcTopics, copyToStreamIdleStrategyFactory, selectedTopicTermBufferLength, effectiveTopicTotalTermBufferLimit);
    }

    /**
     * <a href="https://gitlab.deltixhub.com/Deltix/QuantServer/QuantServer/-/issues/1098#note_700292">...</a>
     */
    private static long getEffectiveTopicTotalTermBufferLimit(@Nullable Long topicTotalTermBufferLimit, boolean enabled, boolean embeddedDriver) {
        if (topicTotalTermBufferLimit != null) {
            if (topicTotalTermBufferLimit == -1) {
                // "-1: in config means "No limit"
                return Long.MAX_VALUE;
            }
            return topicTotalTermBufferLimit;
        }
        if (!enabled) {
            return 0;
        }
        if (embeddedDriver) {
            // For embedded driver we do not use any limit
            return Long.MAX_VALUE;
        }

        throw new IllegalArgumentException("topicTotalTermBufferLimit is not set and external Aeron driver is used. Please set " + SYS_PROP_TOPIC_TERM_BUFFER_LIMIT + " property to explicitly define amount of native memory dedicated to Aeron topics");
    }

    public synchronized void start() {
        if (!enabled) {
            return;
        }

        if (state != State.NOT_STARTED) {
            throw new IllegalStateException("Wrong state: " + state);
        }

        if (startEmbeddedDriver && TDBProtocol.NEEDS_AERON_DRIVER) {
            this.driver = createDriver(this.aeronDir, getThreadFactoryForAeron());
        }

        this.state = State.STARTED;
    }

    @Nonnull
    public synchronized Aeron getAeron() {
        if (!enabled) {
            throw new IllegalStateException("Aeron support is not enabled in TimeBase");
        }
        if (state != State.STARTED) {
            throw new IllegalStateException("Wrong state: " + state);
        }
        if (this.aeron != null && aeron.isClosed()) {
            LOG.warn("Aeron client is closed. Resetting it.");
            aeron = null;
        }
        if (aeron == null) {
            LOG.info("Starting instance of Aeron client at %s").with(this.aeronDir);
            aeron = createAeron(this.aeronDir, getThreadFactoryForAeron());
        }
        return aeron;
    }

    public synchronized void stop() {
        if (!enabled) {
            return;
        }

        if (state != State.STARTED) {
            throw new IllegalStateException("Wrong state: " + state);
        }
        copyThreadsCanRun.set(false); // TODO: We also might need to wait till all threads stop.

        if (aeron != null) {
            aeron.close();
        }
        if (driver != null) {
            driver.close();
            deleteAeronDir(driver.aeronDirectoryName());
        }

        // TODO: close multicastContexts

        this.state = State.STOPPED;
    }

    /**
     * Try to delete directory.
     * Ignore failure.
     */
    private void deleteAeronDir(String dirName) {
        IOUtil.deleteFileOrDir(new File(dirName));
    }

//    private static boolean deleteRecursively(File dir) {
//        try {
//            FileUtils.deleteDirectory(dir);
//            return true;
//        } catch (IOException e) {
//            return false;
//        }
//    }


    private static Aeron createAeron(String aeronDir, @Nullable ThreadFactory threadFactory) {
        Aeron.Context context = new Aeron.Context();
        context.aeronDirectoryName(aeronDir);

        // Set high timeouts to simplify debugging. In fact we don't use Aeron's timeouts.
        context.driverTimeoutMs(TimeUnit.SECONDS.toMillis(DRIVER_COMMUNICATION_TIMEOUT));
        context.errorHandler(throwable -> {
            if (throwable instanceof DriverTimeoutException)
                Util.logException("Timeout from the MediaDriver. Aeron-related functionality is non-functional:", throwable);
            else
                Util.logException("Unhandled Aeron exception:", throwable);
        });

        if (threadFactory != null) {
            context.threadFactory(threadFactory);
        }

        return Aeron.connect(context);
    }

    private static MediaDriver createDriver(String aeronDir, @Nullable ThreadFactory threadFactory) {
        final MediaDriver.Context context = new MediaDriver.Context();

        //* min latency
/*        context.threadingMode(ThreadingMode.DEDICATED)
                .dirsDeleteOnStart(true)
                .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
                .receiverIdleStrategy(new NoOpIdleStrategy())
                .senderIdleStrategy(new NoOpIdleStrategy())
                .sharedIdleStrategy(new NoOpIdleStrategy());*/
        //*/

        // We not use network part of Aeron so no reason for dedicated threads // TODO: Investigate
        context.threadingMode(ThreadingMode.SHARED);
        context.ipcTermBufferLength(IPC_TERM_BUFFER_LENGTH);
        context.aeronDirectoryName(aeronDir);

        context.driverTimeoutMs(TimeUnit.SECONDS.toMillis(DRIVER_COMMUNICATION_TIMEOUT));

        // Set high timeouts to simplify debugging. In fact we don't use Aeron's timeouts.
        if (DEBUG_MODE) {
            context.publicationUnblockTimeoutNs(TimeUnit.MINUTES.toNanos(10));
            context.clientLivenessTimeoutNs(TimeUnit.MINUTES.toNanos(5));
            context.driverTimeoutMs(TimeUnit.MINUTES.toMillis(5));
        }

        if (threadFactory != null) {
            context.conductorThreadFactory(threadFactory);
            context.senderThreadFactory(threadFactory);
            context.receiverThreadFactory(threadFactory);
            context.sharedThreadFactory(threadFactory);
            context.sharedNetworkThreadFactory(threadFactory);
        }

        if (DRIVER_STRATEGY != null) {
            switch (DRIVER_STRATEGY) {
                case "yield":
                    context.sharedIdleStrategy(new YieldingIdleStrategy()); // For low latency.
                    break;
                case "spin":
                    context.sharedIdleStrategy(new BusySpinIdleStrategy()); // For lowest latency.
                    break;
            }
        }

        MediaDriver mediaDriver = MediaDriver.launchEmbedded(context);
        LOG.info("Aeron driver started in directory: %s").with(aeronDir);
        return mediaDriver;
    }

    public int getNextStreamId() {
        return aeronStreamIdGenerator.nextId();
    }

    public IdGenerator getStreamIdGenerator() {
        return aeronStreamIdGenerator;
    }

    @Nullable
    public String getAeronDir() {
        return enabled ? aeronDir : null;
    }

    public AeronMulticastStreamContext subscribeToMulticast(String streamKey, BiFunction<String, AeronMulticastStreamContext, AeronMulticastStreamContext> remappingFunction) {
        synchronized (multicastContexts) {
            return multicastContexts.compute(streamKey, remappingFunction);
        }
    }

    public void unsubscribeFromMulticast(String streamKey, VSChannel channel) {
        synchronized (multicastContexts) {
            AeronMulticastStreamContext multicastContext = multicastContexts.get(streamKey);
            boolean isEmpty = multicastContext.removeSubscriber(channel);
            if (isEmpty) {
                multicastContext.markStopped();
                multicastContexts.remove(streamKey);
            }
        }
    }

    public String getMulticastChannel() {
        StringBuilder s = new StringBuilder().append("aeron:udp?endpoint=").append(MULTICAST_ADDRESS);
        if (MULTICAST_INTERFACE != null) {
            s.append("|interface=").append(MULTICAST_INTERFACE);
        }
        if (MULTICAST_TTL != null) {
            s.append("|ttl=").append(MULTICAST_TTL);
        }
        return s.toString();
    }

    @Nonnull
    private static IdleStrategyFactory createDefaultCopyToStreamIdleStrategy() {
        long waitTimeNs = TimeUnit.MILLISECONDS.toNanos(100);
        return new IdleStrategyFactory(0, 0, waitTimeNs, waitTimeNs);
    }

    public boolean copyThreadsCanRun() {
        return copyThreadsCanRun.get();
    }

    /**
     * Creates a standalone simple in-memory {@link IdGenerator}.
     */
    @Nonnull
    private static IdGenerator createStandaloneIdGenerator() {
        return new IdGenerator() {
            private final AtomicInteger counter = new AtomicInteger(0);

            @Override
            public int nextId() {
                return counter.incrementAndGet();
            }
        };
    }

    @Nullable
    private ThreadFactory getThreadFactoryForAeron() {
        if (affinityConfig == null || affinityConfig.getAffinityLayout() == null) {
            return null;
        }
        // Note: we don't set custom thread names because we expect that Aeron will rename threads anyway
        return new PinnedThreadFactoryWrapper(Thread::new, affinityConfig.getAffinityLayout());
    }

    @Nullable
    public String getPublicAddress() {
        return publicAddress;
    }

    public boolean isAeronEnabled() {
        return enabled;
    }

    public boolean isBypassRemoteCheckForIpcTopics() {
        return bypassRemoteCheckForIpcTopics;
    }

    @Nonnull
    public IdleStrategyFactory getCopyToStreamIdleStrategyFactory() {
        return copyToStreamIdleStrategyFactory;
    }

    public int getDefaultTopicTermBufferLength() {
        return defaultTopicTermBufferLength;
    }

    public long getTopicTotalTermBufferLimit() {
        return topicTotalTermBufferLimit;
    }

    @NotNull
    private static Integer determineTermBufferLength(@Nullable Integer termBufferLengthFromConfig) {
        if (termBufferLengthFromConfig != null) {
            return termBufferLengthFromConfig;
        }
        Integer value1 = Integer.getInteger(SYS_PROP_TERM_BUFFER_LENGTH, null);
        if (value1 != null) {
            return value1;
        }
        // Older parameter name - for compatibility.
        // The name is misleading because it's not for IPC.
        Integer value2 = Integer.getInteger(SYS_PROP_TERM_BUFFER_LENGTH_OLD, null);
        //noinspection ReplaceNullCheck
        if (value2 != null) {
            return value2;
        }

        return Configuration.TERM_BUFFER_LENGTH_DEFAULT; // Use defaults from Aeron
    }

    /**
     * Determines if Aeron client should be enabled with current system properties and provided config.
     * During execution this method will report warnings to log in case of misconfiguration.
     *
     * <p>Rules:</p>
     * <ul>
     *     <li>Check value for {@link #SYS_PROP_TIME_BASE_AERON_ENABLED} in config and system properties</li>
     *     <li>If system and config values are in conflict, then use system value and report a warning</li>
     *     <li>If there is no conflict, use corresponding value.</li>
     *     <li>If there are no any value, enable aeron if external Aeron driver directory is configured.</li>
     *     <li>Otherwise return default value (See {@link #ENABLED_BY_DEFAULT})</li>
     * </ul>
     */
    public static boolean selectEffectiveAeronMode(@Nullable QuantServiceConfig config) {
        Boolean systemOptionValue = null;
        String systemStrValue = System.getProperty(SYS_PROP_TIME_BASE_AERON_ENABLED, null);
        if (systemStrValue != null) {
            systemOptionValue = Boolean.parseBoolean(systemStrValue);
        }

        Boolean configOptionValue = null;
        if (config != null) {
            String strValue = config.getString(null, SYS_PROP_TIME_BASE_AERON_ENABLED, null);
            if (strValue != null) {
                configOptionValue = Boolean.parseBoolean(strValue);
            }
        }

        Boolean result = null;
        if (systemOptionValue != null) {
            if (configOptionValue != null && !systemOptionValue.equals(configOptionValue)) {
                LOG.warn("Aeron enabled value is in conflict between system and config properties. System value (used): %s. Config value (ignored): %s")
                        .with(systemOptionValue).with(configOptionValue);
            }
            result = systemOptionValue;
        }
        if (result == null && configOptionValue != null) {
            result = configOptionValue;
        }

        boolean externalDriver = AeronWorkDirManager.isExternalDriverConfigured();
        if (result != null) {
            if (externalDriver && !result) {
                LOG.warn("Misconfiguration: Aeron is disabled in config but external Aeron driver is configured. Aeron will remain disabled.");
            }
            return result;
        }

        assert result == null;
        if (externalDriver) {
            return true;
        }

        return ENABLED_BY_DEFAULT;
    }

    private enum State {
        NOT_STARTED,
        STARTED,
        STOPPED
    }
}