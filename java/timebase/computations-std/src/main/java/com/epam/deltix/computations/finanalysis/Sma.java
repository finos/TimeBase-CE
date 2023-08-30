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

package com.epam.deltix.computations.finanalysis;

import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;
import com.epam.deltix.containers.generated.DecimalDataQueue;
import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

import javax.naming.OperationNotSupportedException;

@Function("SMA")
public class Sma extends DoubleStatefulFunctionBase {

    private DecimalDataQueue queue;
    private long timePeriod = -1;
    private int period = -1;
    private boolean reset = false;

    @Init
    public void initPeriod(@Arg(defaultValue = "14") int period, @Arg(defaultValue = "false") boolean reset) {
        this.period = period;
        this.timePeriod = -1;
        this.reset = reset;
        this.queue = new DecimalDataQueue(period, true, false);
    }

    @Init
    public void initTimePeriod(long timePeriod, @Arg(defaultValue = "false") boolean reset) {
        this.period = -1;
        this.timePeriod = timePeriod;
        this.reset = reset;
        this.queue = new DecimalDataQueue(timePeriod, true);
    }

    @Compute
    public void set(@BuiltInTimestampMs long timestamp, double v) {
        if (TimebaseTypes.isNull(v)) {
            return;
        }

        try {
            if (timePeriod == -1) {
                queue.put(Decimal64.fromDouble(v));
            } else {
                queue.put(Decimal64.fromDouble(v), timestamp);
            }

            value = queue.arithmeticMean().doubleValue();
        } catch (OperationNotSupportedException ignored) {
        }
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            if (timePeriod == -1) {
                queue = new DecimalDataQueue(period, true, false);
            } else {
                queue = new DecimalDataQueue(timePeriod, true);
            }
        }

        super.reset();
    }

}

