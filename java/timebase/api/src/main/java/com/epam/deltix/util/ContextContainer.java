package com.epam.deltix.util;

import com.google.common.annotations.VisibleForTesting;
import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.util.concurrent.QuickExecutor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
// TODO: rename to ThreadExecutor and move to util

public class ContextContainer {
    private static final AtomicInteger namelessExecutorIndex = new AtomicInteger();
    private static final AtomicInteger testExecutorIndex = new AtomicInteger();

    private volatile AffinityConfig affinityConfig = getDefaultAffinityConfig();

    private volatile String quickExecutorName = null;
    private volatile QuickExecutor quickExecutor = null;

    @Nullable
    public AffinityConfig getAffinityConfig() {
        return affinityConfig;
    }

    public void setAffinityConfig(@Nullable AffinityConfig affinityConfig) {
        this.affinityConfig = affinityConfig;
    }

    @Nonnull
    public QuickExecutor getQuickExecutor() {
        // Lazy singleton
        if (this.quickExecutor == null) {
            synchronized (QuickExecutor.class) {
                if (this.quickExecutor == null) {
                    this.quickExecutor = createQuickExecutor();
                }
            }
        }
        return this.quickExecutor;
    }

    /**
     * Marks QuickExecutor for shutdown. Tries to avoid executor creation.
     */
    public void shutdownQuickExecutor() {
        // Note: current implementation DOES breaks instance creation check.
        if (this.quickExecutor == null) {
            return;
        }
        getQuickExecutor().shutdownInstance();
    }

    @Nonnull
    private QuickExecutor createQuickExecutor() {
        String name = this.quickExecutorName;
        if (name == null) {
            name = "ContextContainer Executor #" + namelessExecutorIndex.incrementAndGet();
        }
        return QuickExecutor.createNewInstance(name, this.affinityConfig);
    }

    public void setQuickExecutorName(String quickExecutorName) {
        this.quickExecutorName = quickExecutorName;
    }


    @VisibleForTesting
    public static ContextContainer getContextContainerForClientTests() {
        ContextContainer contextContainer = new ContextContainer();
        contextContainer.setQuickExecutorName("Client Test Executor #" + testExecutorIndex.incrementAndGet());
        return contextContainer;
    }

    @VisibleForTesting
    public static ContextContainer getContextContainerForServerTests() {
        ContextContainer contextContainer = new ContextContainer();
        contextContainer.setQuickExecutorName("Server Test Executor #" + testExecutorIndex.incrementAndGet());
        return contextContainer;
    }

    private static AffinityConfig getDefaultAffinityConfig() {
        // Null value mean no any explicit affinity
        return null;
    }
}
