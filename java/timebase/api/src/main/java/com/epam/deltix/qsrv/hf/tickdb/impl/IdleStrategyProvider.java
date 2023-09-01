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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
public final class IdleStrategyProvider {
    public static IdleStrategy getIdleStrategy(ChannelPerformance channelPerformance) {
        switch (channelPerformance) {
            case MIN_CPU_USAGE:
            default:
                return new BackoffIdleStrategy(1, 1, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(1000));

            case LOW_LATENCY:
                // Yield a bit and switch to short waits
                return new BackoffIdleStrategy(1, 100, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(10));

            case LATENCY_CRITICAL:
                // For Java 9 it's better to spin with Thread.onSpinWait() (see http://openjdk.java.net/jeps/285)
                return new BusySpinIdleStrategy();
        }
    }
}