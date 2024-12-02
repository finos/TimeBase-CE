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


import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.NoOpIdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;
import org.agrona.concurrent.YieldingIdleStrategy;

import java.util.concurrent.TimeUnit;


public class IdleStrategyFactory {

    private final long maxSpins;
    private final long maxYields;

    //@Duration(timeUnit = TimeUnit.NANOSECONDS)
    private final long minParkPeriod;

    //@Duration(timeUnit = TimeUnit.NANOSECONDS)
    private final long maxParkPeriod;

    public IdleStrategyFactory(long maxSpins, long maxYields, long minParkPeriod, long maxParkPeriod) {
        this.maxSpins = maxSpins;
        this.maxYields = maxYields;
        this.minParkPeriod = minParkPeriod;
        this.maxParkPeriod = maxParkPeriod;
    }

    boolean hasMaxSpins() {
        return maxSpins > 0;
    }

    boolean hasMaxYields() {
        return maxYields > 0;
    }

    boolean hasMinParkPeriod() {
        return minParkPeriod > 0;
    }

    boolean hasMaxParkPeriod() {
        return maxParkPeriod > 0;
    }

    public IdleStrategy create() {
        if (!hasMaxSpins() && !hasMaxYields() && !hasMinParkPeriod() && !hasMaxParkPeriod()) {
            return NoOpIdleStrategy.INSTANCE;
        }

        if (hasMaxSpins() && !hasMaxYields() && !hasMinParkPeriod() && !hasMaxParkPeriod()) {
            return new BusySpinIdleStrategy();
        }

        if (!hasMaxSpins() && hasMaxYields() && !hasMinParkPeriod() && !hasMaxParkPeriod()) {
            return new YieldingIdleStrategy();
        }

        if (!hasMaxSpins() && !hasMaxYields() && hasMinParkPeriod()) {
            if (!hasMaxParkPeriod() || minParkPeriod == maxParkPeriod) {
                return new SleepingIdleStrategy(minParkPeriod);
            }
        }

        return new BackoffIdleStrategy(maxSpins, maxYields, minParkPeriod, maxParkPeriod);
    }

}