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
package com.epam.deltix.computations.stateful.window;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.arrays.DoubleArrayStatefulFunction;
import com.epam.deltix.computations.stateful.window.containers.WindowDoubleArrayList;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.util.collections.generated.DoubleArrayList;

@Function("WINDOW")
public class WindowDouble implements DoubleArrayStatefulFunction {

    private final static int MAX_WINDOWS_SIZE = Integer.getInteger("TimeBase.qql.maxWindowSize", 1_000_000);

    private WindowDoubleArrayList window;
    private boolean reset;
    private boolean skipNull = true;

    @Init
    public void init(@Arg(name = "period") int period, @Arg(name = "reset", defaultValue = "false") boolean reset) {
        this.window = new WindowDoubleArrayList(period);
        this.reset = reset;

        if (period > MAX_WINDOWS_SIZE) {
            throw new IllegalArgumentException("Invalid period (" + period + "). Max windows size is " + MAX_WINDOWS_SIZE);
        }

        if (period <= 0) {
            throw new IllegalArgumentException("Invalid period: " + period);
        }

    }

    @Init
    public void init(@Arg(name = "timePeriod") long timePeriod, @Arg(name = "reset", defaultValue = "false") boolean reset) {
        this.window = new WindowDoubleArrayList(timePeriod);
        this.reset = reset;
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        if (skipNull && TimebaseTypes.isNull(v)) {
            return;
        }
        window.add(v, timestamp);
        if (window.size() > MAX_WINDOWS_SIZE) {
            throw new IllegalArgumentException("Invalid windows size (" + window.size() + "). Max windows size is " + MAX_WINDOWS_SIZE);
        }
    }

    @Override
    @Reset
    public void reset() {
        if (reset) {
            window.clear();
        }
    }

    @Result
    @Decimal
    public DoubleArrayList get() {
        return window.getValues();
    }

}