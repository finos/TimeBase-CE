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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron;

import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.IdGenerator;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.multicast.AeronMulticastStreamContext;
import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.thread.affinity.PinnedThreadFactoryWrapper;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.vsocket.VSChannel;
import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import io.aeron.exceptions.DriverTimeoutException;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

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
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class DXServerAeronContext {
    // Aeron driver communication timeout, in seconds
    // This timeout defines how fast we detect that driver is unavailable
    // Too low value may break service operation after GC or a pause caused by debug
    // Too high value may cause Aeron client to not detect that driver (or local timebase) is down for long time
    // If you want to debug application that uses topics you should set this value to a higher value (like 300, i.e. 5 minutes)
    public static final int DRIVER_COMMUNICATION_TIMEOUT = Integer.getInteger("TimeBase.transport.aeron.driverTimeout", 300);

    // We need 4x IPC_TERM_BUFFER_LENGTH memory per cursor and 2x IPC_TERM_BUFFER_LENGTH memory per loader.
    // So increase with caution.

    // Buffer size for Aeron-based cursors and loaders.
    public static final int IPC_TERM_BUFFER_LENGTH = Integer.getInteger("TimeBase.transport.aeron.ipc.term.buffer.length", 2 * 1024 * 1024); // 2Mb default

    private static final String DRIVER_STRATEGY = System.getProperty("TimeBase.transport.aeron.driverStrategy");

    // Buffer size for Aeron based topics (per topic size).
    public static final int TOPIC_IPC_TERM_BUFFER_LENGTH = Integer.getInteger("TimeBase.transport.aeron.topic.ipc.term.buffer.length", 16 * 1024 * 1024); // 16 Mb default

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

    public static final int SINGLE_PUBLISHER_TOPIC_METADATA_PUBLISHER_PORT = Integer.getInteger("TimeBase.transport.aeron.topic.udp.single.metadata.publisher.default.port", 40493);
    public static final int SINGLE_PUBLISHER_TOPIC_METADATA_SUBSCRIBER_PORT = Integer.getInteger("TimeBase.transport.aeron.topic.udp.single.metadata.subscriber.default.port", 40494);

    // Sets id range to be used for Aeron stream ID. If set then TB will use only values from the specified range for Aeron Stream IDs.
    // Last generated ID will be written into a file after each new stream created.
    // This option usually makes sense only when used together with "TimeBase.transport.aeron.external.driver.dir" option.
    // In that case it's important to ensure that different applications that use same shared Aeron driver do not have overlapping Stream ID ranges.
    // Value must contain two integer numbers (lowest and highest possible values) separated by ":". Values may be negative.
    // Example value: "-100000000:299999999".
    private static final String RANGE_PROP_NAME = "TimeBase.transport.aeron.id.range";
    public static final String ID_RANGE = System.getProperty(RANGE_PROP_NAME, null);

    private final String aeronDir;
    private final boolean startDriver;

    private final String publicAddress;

    private State state;
    private MediaDriver driver;
    private Aeron aeron;
    private final AffinityConfig affinityConfig;

    private final Map<String, AeronMulticastStreamContext> multicastContexts = new HashMap<>();

    private static final IdGenerator aeronStreamIdGenerator = createIdGenerator();

    private final AtomicBoolean copyThreadsCanRun = new AtomicBoolean(true);

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

    public DXServerAeronContext(String aeronDir, boolean startEmbeddedDriver, @Nullable AffinityConfig affinityConfig, @Nullable String publicAddress) {
        this.aeronDir = aeronDir;
        this.startDriver = startEmbeddedDriver;
        this.affinityConfig = affinityConfig;
        this.state = State.NOT_STARTED;
        this.publicAddress = publicAddress;
    }

    public static DXServerAeronContext createDefault(int tickDbPort, @Nullable AffinityConfig affinityConfig, @Nullable String publicAddress) {
        String aeronDir = AeronWorkDirManager.setupWorkingDirectory(tickDbPort, System.currentTimeMillis());
        return new DXServerAeronContext(aeronDir, AeronWorkDirManager.useEmbeddedDriver(), affinityConfig, publicAddress);
    }

    public synchronized void start() {
        if (state != State.NOT_STARTED) {
            throw new IllegalStateException("Wrong state: " + state);
        }

        if (startDriver && TDBProtocol.NEEDS_AERON_DRIVER) {
            this.driver = createDriver(this.aeronDir, getThreadFactoryForAeron());
        }

        this.state = State.STARTED;
    }

    @Nonnull
    public synchronized Aeron getAeron() {
        if (state != State.STARTED) {
            throw new IllegalStateException("Wrong state: " + state);
        }
        if (aeron == null) {
            aeron = createAeron(this.aeronDir, getThreadFactoryForAeron());
        }
        return aeron;
    }

    public synchronized void stop() {
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
        //noinspection ResultOfMethodCallIgnored
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

        // Set high timeouts to simplify debugging. In fact we don't use Aeron's timeouts.
        //context.clientLivenessTimeoutNs(TimeUnit.MINUTES.toNanos(5));
        context.driverTimeoutMs(TimeUnit.SECONDS.toMillis(DRIVER_COMMUNICATION_TIMEOUT));

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

        return MediaDriver.launchEmbedded(context);
    }

    public int getNextStreamId() {
        return aeronStreamIdGenerator.nextId();
    }

    public IdGenerator getStreamIdGenerator() {
        return aeronStreamIdGenerator;
    }

    public String getAeronDir() {
        return aeronDir;
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

    private enum State {
        NOT_STARTED,
        STARTED,
        STOPPED
    }
}