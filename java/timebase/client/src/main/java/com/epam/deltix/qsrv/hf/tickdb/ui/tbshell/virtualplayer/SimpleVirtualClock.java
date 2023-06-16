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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.qsrv.hf.pub.TimeSource;

/**
 * Implements virtual clock that may run with specified speed and from specified virtual time.
 * Speed may be changed dynamically.
 * Clock may be paused.
 *
 * @author Alexei Osipov
 */
public class SimpleVirtualClock implements TimeSource {
    // Configuration
    private double speed;
    private final TimeSource baseTimeSource; // Time source to act as "real time"

    // Time
    private long baseRealTime;
    private long baseVirtualTime;

    private boolean paused = false;

    public SimpleVirtualClock(double speed, TimeSource baseTimeSource) {
        this.speed = speed;
        this.baseTimeSource = baseTimeSource;
        // Initialize clock with current time
        this.baseRealTime = this.baseTimeSource.currentTimeMillis();
        this.baseVirtualTime = this.baseRealTime;
    }

    /**
     * @return current virtual time
     */
    @Override
    public long currentTimeMillis() {
        long currentSystemTime = baseTimeSource.currentTimeMillis();
        return getVirtualTimeFromRealTime(currentSystemTime);
    }

    public long getVirtualTimeFromRealTime(long currentRealTime) {
        return Math.round(getEffectiveSpeed() * (currentRealTime - baseRealTime) + baseVirtualTime);
    }

    /**
     * Sets current virtual clock time to a specific timestamp.
     */
    public void resetTo(long startingVirtualTimestamp) {
        baseRealTime = baseTimeSource.currentTimeMillis();
        baseVirtualTime = startingVirtualTimestamp;
        paused = false;
    }

    public void setSpeed(double speed) {
        updateBase();
        this.speed = speed;
    }

    public boolean pause() {
        if (isPaused()) {
            return false;
        }
        updateBase();
        paused = true;
        return true;
    }

    public boolean resume() {
        if (!isPaused()) {
            return false;
        }
        updateBase();
        paused = false;
        return true;
    }


    public boolean isPaused() {
        return paused;
    }

    /**
     * @return actual current speed, i.e. 0 if paused
     */
    private double getEffectiveSpeed() {
        return paused ? 0 : speed;
    }

    private void updateBase() {
        long currentRealTime = baseTimeSource.currentTimeMillis();
        long currentVirtualTime = getVirtualTimeFromRealTime(currentRealTime);
        baseRealTime = currentRealTime;
        baseVirtualTime = currentVirtualTime;
    }

    /**
     * @return configured speed (may be non 0 even if paused)
     */
    public double getSpeed() {
        return speed;
    }

    /**
     * @return true if clock is not stopped (due to pause or zero speed)
     */
    public boolean isTimeRunning() {
        return !isPaused() && speed != 0;
    }
}