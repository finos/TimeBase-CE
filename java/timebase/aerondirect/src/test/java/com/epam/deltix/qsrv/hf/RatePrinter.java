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
package com.epam.deltix.qsrv.hf;

import com.epam.deltix.util.time.TimeKeeper;

import java.util.concurrent.TimeUnit;

/**
 * <p>Utility class for measurement of invocation rate.
 * With this class you can measure how much time some event occurred during given interval.
 * This class is designed for case with millions messages per second.
 *
 * See also {@link deltix.util.time.PrintingStopWatch}.
 * See also {@link deltix.util.io.aeron.PrintingCounter}.
 * @author Alexei Osipov
 */
public final class RatePrinter {
    private static final boolean ENABLED = Boolean.getBoolean("deltix.qsrv.hf.RatePrinter.enabled"); // NOTE: MUST NOT BE ENABLED IN PROD!

    private final static long timeIntervalMs = TimeUnit.SECONDS.toMillis(5); // Edit this value if you
    private final static int checkTimeEachMessages = 1_000;

    private long count;
    private long startTime = TimeKeeper.currentTime;
    private long prevTime = startTime;

    private final String name;

    /**
     * @param name Name for the StopWatch. Will be included into the printed log.
     */
    public RatePrinter(String name) {
        this.name = name;
    }

    /**
     * Call this method to start measurement.
     */
    public void start() {
        if (!ENABLED) {
            return;
        }
        startTime = TimeKeeper.currentTime;
        prevTime = startTime;
        count = 0;
    }

    public void inc() {
        if (!ENABLED) {
            return;
        }
        count += 1;
        if (count % checkTimeEachMessages == 0) {
            long currentTime = TimeKeeper.currentTime;
            long timeDelta = currentTime - prevTime;

            if (timeDelta > timeIntervalMs) {
                long secondsFromStart = (currentTime - startTime) / 1000;
                System.out.printf("%6d: %s: Rate: %.3f k msg/s\n", secondsFromStart, name, ((float) count) / timeDelta);
                prevTime = currentTime;
                count = 0;
            }
        }
    }
}