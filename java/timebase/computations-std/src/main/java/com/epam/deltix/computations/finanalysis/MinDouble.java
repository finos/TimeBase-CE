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
package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.finanalysis.util.MinMaxQueue;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

@Function("MIN")
public class MinDouble {

    private MinMaxQueue queue;
    private boolean needTimestamp;

    private long timePeriod = -1;
    private int period = -1;

    @Init
    public void init(long timePeriod) {
        this.timePeriod = timePeriod;
        queue = new MinMaxQueue(timePeriod, MinMaxQueue.MODE.MINIMUM);
        needTimestamp = true;
    }

    @Init
    public void init(int period) {
        this.period = period;
        queue = new MinMaxQueue(period, MinMaxQueue.MODE.MINIMUM);
        needTimestamp = false;
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double value) {
        if (TimebaseTypes.isNull(value)) {
            return;
        }

        if (needTimestamp) {
            queue.put(value, timestamp);
        } else {
            queue.put(value);
        }
    }

    @Result
    public double result() {
        return queue.extremum();
    }

    @Reset
    public void reset() {
        if (period == -1) {
            queue = new MinMaxQueue(timePeriod, MinMaxQueue.MODE.MINIMUM);
        } else {
            queue = new MinMaxQueue(period, MinMaxQueue.MODE.MINIMUM);
        }
    }

}

