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

import com.epam.deltix.computations.finanalysis.util.MmaProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;

@Function("MMA")
public class Mma extends DoubleStatefulFunctionBase {

    private int period;
    private boolean reset;
    private MmaProcessor mma;

    public void init(@Arg(defaultValue = "14") int period, @Arg(defaultValue = "false") boolean reset) {
        this.period = period;
        this.reset = reset;
        mma = new MmaProcessor(period);
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        mma.add(v, timestamp);
        value = mma.value;
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            mma = new MmaProcessor(period);
        }
        super.reset();
    }
}

