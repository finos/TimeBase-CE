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
 * Clock that just show chosen time.
 * To change time you have to do that manually.
 *
 * @author Alexei Osipov
 */
// TODO: Move this class to time-related utility classes.
public class ManualClock implements TimeSource {
    // Note: this field should not be updated from multiple threads without proper synchronization.
    private volatile long currentTimeMillis = 0;

    @Override
    public long currentTimeMillis() {
        return currentTimeMillis;
    }

    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

    public void advanceByMillis(long timeIntervalMs) {
        this.currentTimeMillis += timeIntervalMs;
    }
}