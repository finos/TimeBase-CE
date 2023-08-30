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
package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;
import com.epam.deltix.util.io.idlestrat.adapter.IdleStrategyAdapter;
import org.agrona.concurrent.BackoffIdleStrategy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

/**
 * Base class for fluent-style API for setting up topic channel preferences.
 *
 * @author Alexei Osipov
 */
@SuppressWarnings("unchecked")
public abstract class TopicChannelPreferences<T extends TopicChannelPreferences> extends ChannelPreferences {
    public TopicChannelPreferences() {
    }

    /**
     * @param raw input message type ({@code true} means {@link deltix.qsrv.hf.pub.RawMessage}
     */
    public T setRaw(boolean raw) {
        this.raw = raw;
        return (T) this;
    }

    /**
     * @param typeLoader type loader to be used for message
     */
    public T setTypeLoader(TypeLoader typeLoader) {
        this.typeLoader = typeLoader;
        return (T) this;
    }

    public T setChannelPerformance(ChannelPerformance channelPerformance) {
        this.channelPerformance = channelPerformance;
        return (T) this;
    }

    /**
     * Populates this object with values from provided {@link ChannelPreferences}.
     */
    public T copyFrom(ChannelPreferences channelPreferences) {
        this.setRaw(channelPreferences.raw);
        this.setTypeLoader(channelPreferences.getTypeLoader());
        this.setChannelPerformance(channelPreferences.channelPerformance);
        return (T) this;
    }

    /**
     * Determines {@link IdleStrategy} to be used for this channel (if applicable).
     * If a non-null {@link IdleStrategy} provided as a parameter then it is returned as is.
     * Otherwise {@link IdleStrategy} is determined by {@link #channelPerformance}.
     */
    @Nonnull
    public IdleStrategy getEffectiveIdleStrategy(@Nullable IdleStrategy idleStrategy) {
        if (idleStrategy != null) {
            return idleStrategy;
        }

        switch (channelPerformance) {
            case MIN_CPU_USAGE:
                return IdleStrategyAdapter.adapt(new BackoffIdleStrategy(1, 1, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(1000)));

            case LATENCY_CRITICAL:
                // For Java 9 it's better to spin with Thread.onSpinWait() (see http://openjdk.java.net/jeps/285)
                return new BusySpinIdleStrategy();

            default:
                return new YieldingIdleStrategy();
        }

    }
}