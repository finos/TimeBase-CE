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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicTransferType;
import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.thread.affinity.PinnedThreadFactoryWrapper;
import com.epam.deltix.util.lang.StringUtils;
import io.aeron.Aeron;
import io.aeron.exceptions.DriverTimeoutException;
import net.jcip.annotations.GuardedBy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This class contains Aeron-specific client instance bound data.
 *
 * Client context permits restart.
 *
 * @author Alexei Osipov
 */
public class DXClientAeronContext {
    public static final String PROP_NAME_AERON_DIRECTORY = "TimeBase.transport.aeron.directory";
    public static final String ENV_VAR_AERON_DIR = "AERON_DIR";

    // Aeron driver communication timeout, in seconds
    // This timeout defines how fast we detect that driver is unavailable
    // Too low value may break service operation after GC or a pause caused by debug
    // Too high value may cause Aeron client to not detect that driver (or local timebase) is down for long time
    // If you want to debug application that uses topics you should set this value to a higher value (like 300, i.e. 5 minutes)
    public static final int DRIVER_COMMUNICATION_TIMEOUT = Integer.getInteger("TimeBase.transport.aeron.driverTimeout", 60);

    private static final Log LOG = LogFactory.getLog(DXClientAeronContext.class.getName());

    private String aeronDir = null;
    private final String aeronForRemoteServerDir = getAeronDirForRemoteServer();

    public static String getAeronDirForRemoteServer() {
        String property = System.getProperty(PROP_NAME_AERON_DIRECTORY);
        if (!StringUtils.isEmpty(property))
            return property;

        String envValue = System.getenv(ENV_VAR_AERON_DIR);
        if (!StringUtils.isEmpty(envValue))
            return envValue;
        return null;
    }

    private State state;
    private Aeron aeron;
    private Aeron aeronForRemoteServer;
    private AffinityConfig affinityConfig;

    private DXAeronSubscriptionChecker subscriptionChecker = null;

    public DXClientAeronContext(@Nullable AffinityConfig affinityConfig) {
        this.affinityConfig = affinityConfig;
        this.state = State.NOT_STARTED;
    }

    public synchronized void start() {
        if (state != State.NOT_STARTED && state != State.STOPPED) {
            throw new IllegalStateException("Wrong state: " + state);
        }

        this.state = State.STARTED;
        assert this.subscriptionChecker == null;
        this.subscriptionChecker = new DXAeronSubscriptionChecker();
    }

    /**
     * Get (or initialize) Aeron instance for communication with local server (via IPC or UDP).
     * Note: this can be used only if client and server on same machine.
     *
     * @param aeronDir aeron work directory, shared with server
     */
    @Nonnull
    public synchronized Aeron getServerSharedAeronInstance(@Nonnull String aeronDir) {
        if (state != State.STARTED) {
            throw new IllegalStateException("Wrong state: " + state);
        }

        if (this.aeronDir != null && !this.aeronDir.equals(aeronDir)) {
            LOG.warn().append("Resetting aeron client due to directory change. Old dir: ").append(this.aeronDir)
                    .append(" New dir: ").appendLast(aeronDir);
            reset();
        }
        if (this.aeron != null && aeron.isClosed()) {
            LOG.warn("Aeron client is closed. Resetting it.");
            reset();
        }

        if (this.aeron == null) {
            this.aeronDir = aeronDir;
            LOG.info("Starting instance of Aeron client (shared) at %s").with(this.aeronDir);
            this.aeron = createAeron(this.aeronDir, getThreadFactoryForAeron());
        }
        return aeron;
    }

    /**
     * Get (or initialize) Aeron instance for explicitly provided Aeron driver directory. See {@link #isAeronDirSetExplicitly()}
     */
    @Nonnull
    public synchronized Aeron getStandaloneAeronInstance() {
        if (state != State.STARTED) {
            throw new IllegalStateException("Wrong state: " + state);
        }

        assertExternalDriverConfigured();

        if (this.aeronForRemoteServer == null) {
            LOG.info("Starting instance of Aeron client (standalone) at %s").with(this.aeronForRemoteServerDir);
            // Start Aeron client
            this.aeronForRemoteServer = createAeron(aeronForRemoteServerDir, getThreadFactoryForAeron());
        }
        return aeronForRemoteServer;
    }

    private void assertExternalDriverConfigured() {
        if (aeronForRemoteServerDir == null) {
            throw new IllegalStateException("Client is not configured for interaction with remote Aeron server. Specify " +
                    ENV_VAR_AERON_DIR + " environment variable or " + PROP_NAME_AERON_DIRECTORY + " property");
        }
    }

    /**
     * @return true if a directory with Aeron Driver was explicitly provided to client
     * using {@link #PROP_NAME_AERON_DIRECTORY} system property or {@link #ENV_VAR_AERON_DIR} env variable.
     */
    public boolean isAeronDirSetExplicitly() {
        return aeronForRemoteServerDir != null;
    }

    public synchronized void reset() {
        closeAeron();
    }


    public synchronized void stop() {
        if (state != State.STARTED) {
            throw new IllegalStateException("Wrong state: " + state);
        }

        subscriptionChecker.stop();
        subscriptionChecker = null;

        closeAeron();

        this.state = State.STOPPED;
    }

    @GuardedBy("this")
    private void closeAeron() {
        if (aeron != null) {
            aeron.close();
            aeron = null;
            aeronDir = null;
            LOG.trace().appendLast("Aeron client was closed");
        }
        if (aeronForRemoteServer != null) {
            aeronForRemoteServer.close();
            aeronForRemoteServer = null;
            LOG.trace().appendLast("Aeron client for remote server was closed");
        }
    }

    public synchronized void stopIfStarted() {
        if (isStarted()) {
            stop();
        }
    }

    public synchronized boolean isStarted() {
        return state == State.STARTED;
    }



    @Nonnull
    public synchronized DXAeronSubscriptionChecker getSubscriptionChecker() {
        if (subscriptionChecker == null) {
            throw new IllegalStateException();
        }
        return subscriptionChecker;
    }

    private static Aeron createAeron(String aeronDir, @Nullable ThreadFactory threadFactory) {
        Aeron.Context context = new Aeron.Context();
        context.aeronDirectoryName(aeronDir);

        context.driverTimeoutMs(TimeUnit.SECONDS.toMillis(DRIVER_COMMUNICATION_TIMEOUT));
        context.errorHandler(throwable -> {
            LOG.log(LogLevel.ERROR).append("Unhandled Aeron exception: ").appendLast(throwable);
            if (throwable instanceof DriverTimeoutException) {
                LOG.log(LogLevel.FATAL).appendLast("Timeout from the MediaDriver. Aeron-related functionality is non-functional");
            }
        });

        if (threadFactory != null) {
            context.threadFactory(threadFactory);
        }

        return Aeron.connect(context);
    }

    @Nullable
    private ThreadFactory getThreadFactoryForAeron() {
        if (affinityConfig == null || affinityConfig.getAffinityLayout() == null) {
            return null;
        }
        // Note: we don't set custom thread names because we expect that Aeron will rename thread anyway
        return new PinnedThreadFactoryWrapper(Thread::new, affinityConfig.getAffinityLayout());
    }

    public synchronized void setAffinityConfig(AffinityConfig affinityConfig) {
        this.affinityConfig = affinityConfig;
    }

    @Nonnull
    public Aeron getAeronInstance(@Nullable String serverProvidedAeronDir, @Nonnull TopicTransferType transferType) {
        // Use client specific driver if configured
        if (isAeronDirSetExplicitly()) {
            return getStandaloneAeronInstance();
        }

        if (serverProvidedAeronDir != null) {
            // Use server provided Aeron driver (server must be local)
            return getServerSharedAeronInstance(serverProvidedAeronDir);
        }

        // Generate errors according to transfer type

        switch (transferType) {
            case UDP:
                // Use client-specific driver
                assertExternalDriverConfigured(); // Will throw exception here
                throw new IllegalStateException("Must not happen");
            case IPC:
                throw new IllegalStateException("IPC transfer type requires explicitly set Aeron driver dir on client or enabled Aeron on local TimeBase instance");
            default:
                throw new IllegalArgumentException("Unknown transfer type: " + transferType);
        }
    }

    private enum State {
        NOT_STARTED,
        STARTED,
        STOPPED
    }
}