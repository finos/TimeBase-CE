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
package com.epam.deltix.util.io.aeron;

import com.epam.deltix.util.time.TimeKeeper;

import java.util.concurrent.TimeUnit;

/**
 * <p>Utility class for simple performance measurements (mainly for debug only).
 *
 * See also {@link deltix.util.time.PrintingStopWatch}.
 * @author Alexei Osipov
 */
public final class PrintingCounter {
    private static final boolean ENABLED = false; // NOTE: MUST NOT BE ENABLED IN PROD!

    private static final long REPORT_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(10); // Edit this value if you want other report interval.

    private long minTime = Long.MAX_VALUE; // Nanos
    private long maxTime = 0;  // Nanos

    private long startTime; // Nanos
    private long totalTime = 0; // Nanos
    private long prevTotalTime = 0; // Nanos

    private long lastReport = 0; // Nanos
    private int counter = 0;
    private int prevCounter = 0;

    private long value = 0;
    private long prevValue = 0;

    private final String name;

    /**
     * @param name Name for the StopWatch. Will be included into the printed log.
     */
    public PrintingCounter(String name) {
        this.name = name;
    }

    /**
     * Call this method to start measurement.
     */
    public void start() {
        if (!ENABLED) {
            return;
        }
        startTime = TimeKeeper.currentTimeNanos;
    }

    public void add(long diff) {
        value += diff;
        long currentTime = TimeKeeper.currentTimeNanos;
        if (currentTime - lastReport > REPORT_INTERVAL_NANOS) {
            // TODO: It's not right to reset start time. We should have a dedicated field for that case.
            long timeForLastInvocation = currentTime - startTime;
            startTime = currentTime;
            totalTime += timeForLastInvocation;
            printReport(currentTime);
        }
    }

    /**
     * Call this method to stop measurement.
     * <p>If previous measurement report
     */
    public void stop() {
        if (!ENABLED) {
            return;
        }

        long stopTime = TimeKeeper.currentTimeNanos;
        long timeForLastInvocation = stopTime - startTime;
        totalTime += timeForLastInvocation;
        counter++;

        if (timeForLastInvocation < minTime) {
            minTime = timeForLastInvocation;
        }
        if (timeForLastInvocation > maxTime) {
            maxTime = timeForLastInvocation;
        }

        if (stopTime - lastReport > REPORT_INTERVAL_NANOS) {
            printReport(stopTime);
        }
    }

    private void printReport(long currentTimeNanos) {
        long totalTimeMs = TimeUnit.NANOSECONDS.toMillis(totalTime);
        long timeDeltaNs = totalTime - prevTotalTime;
        long countDelta = counter - prevCounter;
        long valueDelta = value - prevValue;
        System.out.println("Watch=" + name + ". TT = " + totalTimeMs + " ms. Avg. value/time = " + (1000 * valueDelta / timeDeltaNs) + " M/s.");
        lastReport = currentTimeNanos;
        prevTotalTime = totalTime;
        prevCounter = counter;
        prevValue = value;
        minTime = Long.MAX_VALUE;
        maxTime = 0;
    }
}