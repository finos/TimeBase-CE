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
package com.epam.deltix.computations.stateful;

import com.epam.deltix.computations.api.annotations.BuiltInTimestampMs;
import com.epam.deltix.computations.api.annotations.Compute;
import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.Reset;
import com.epam.deltix.computations.api.generated.DecimalToDecimalStatefulFunctionBase;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

import static com.epam.deltix.dfp.Decimal64Utils.*;

@Function("AVG")
public class AvgDecimal extends DecimalToDecimalStatefulFunctionBase {

    private long count = 0;

    @Compute
    @Override
    public void compute(@BuiltInTimestampMs long timestamp, @Decimal long v) {
        if (Decimal64Utils.isNaN(v)) {
            return;
        }
        if (TimebaseTypes.isDecimalNull(value)) {
            value = Decimal64Utils.ZERO;
        }
        value = add(value, divide(subtract(v, value), Decimal64Utils.fromLong(++count)));
    }

    @Reset
    @Override
    public void reset() {
        count = 0;
        super.reset();
    }
}