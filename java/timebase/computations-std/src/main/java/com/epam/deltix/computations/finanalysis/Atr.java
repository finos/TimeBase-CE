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

import com.epam.deltix.computations.finanalysis.util.AtrProcessor;
import com.epam.deltix.computations.api.annotations.BuiltInTimestampMs;
import com.epam.deltix.computations.api.annotations.Compute;
import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;

@Function("ATR")
public class Atr extends DoubleStatefulFunctionBase implements BarFunction {

    private AtrProcessor atr;

    public void init(int period) {
        atr = new AtrProcessor(period);
    }

    @Compute
    @Override
    public void set(@BuiltInTimestampMs long timestamp, double open, double high, double low, double close, double volume) {
        atr.add(open, high, low, close, volume, timestamp);
        value = atr.atr;
    }
}

