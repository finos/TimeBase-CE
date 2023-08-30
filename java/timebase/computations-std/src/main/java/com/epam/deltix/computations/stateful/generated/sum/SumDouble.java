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
package com.epam.deltix.computations.stateful.generated.sum;

import com.epam.deltix.computations.api.annotations.BuiltInTimestampMs;
import com.epam.deltix.computations.api.annotations.Compute;
import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.generated.DoubleToDoubleStatefulFunctionBase;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

@Function("SUM")
public class SumDouble extends DoubleToDoubleStatefulFunctionBase {

    @Compute
    @Override
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        if (TimebaseTypes.isNull(v)) {
            return;
        }
        if (TimebaseTypes.isNull(value)) {
            value = v;
        } else {
            value += v;
        }
    }

}