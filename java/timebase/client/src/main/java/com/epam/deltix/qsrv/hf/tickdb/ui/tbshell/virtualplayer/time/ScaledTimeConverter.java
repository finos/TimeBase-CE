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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer.time;

import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.util.concurrent.TimeUnit;

/**
 * Converts message time in a liner way.
 * Rule: time that was N seconds away from "srcInitialTime" will become N/S seconds away from "dstInitialTime", where S is "virtual speed".
 *
 * For example:
 * 1) Time at srcInitialTime is always converted to dstInitialTime
 * 2) If speed is 5, then message with timestamp srcInitialTime + 10 will get timestamp dstInitialTime + 2.
 *
 * @author Alexei Osipov
 */
public class ScaledTimeConverter implements MessageTimeConverter {
    private static final long nanosInMs = TimeUnit.MILLISECONDS.toNanos(1);


    private final long srcInitialTimeNanos;
    private final long dstInitialTimeNanos;
    private final double speed;

    public ScaledTimeConverter(long srcInitialTime, long dstInitialTime, double speed) {
        this.srcInitialTimeNanos = srcInitialTime * nanosInMs;
        this.dstInitialTimeNanos = dstInitialTime * nanosInMs;
        this.speed = speed;
    }

    @Override
    public void convertTime(InstrumentMessage message) {
        long originalNanoTime = message.getNanoTime();
        long timePassedNanos = originalNanoTime - srcInitialTimeNanos;
        long destTimePassedNanos = (long) (timePassedNanos / speed);
        long destNanoTime = dstInitialTimeNanos + destTimePassedNanos;
        message.setNanoTime(destNanoTime);
    }
}