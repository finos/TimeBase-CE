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

import com.epam.deltix.computations.finanalysis.util.EmaProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

@Function("EMA")
public class Ema extends DoubleStatefulFunctionBase {

    private EmaProcessor ema;
    private int period = -1;
    private double factor = -1;
    private boolean reset = false;

    @Init
    public void init(@Arg(defaultValue = "14") int period, @Arg(defaultValue = "false") boolean reset) {
        this.period = period;
        this.reset = reset;
        ema = new EmaProcessor(period);
    }

    @Init
    public void init(double factor, @Arg(defaultValue = "false") boolean reset) {
        this.factor = factor;
        this.reset = reset;
        ema = new EmaProcessor(factor);
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        if (TimebaseTypes.isNull(v)) {
            return;
        }
        ema.add(v, timestamp);
        value = ema.value;
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            if (period == -1) {
                ema = new EmaProcessor(factor);
            } else {
                ema = new EmaProcessor(period);
            }
        }
        super.reset();
    }

}
