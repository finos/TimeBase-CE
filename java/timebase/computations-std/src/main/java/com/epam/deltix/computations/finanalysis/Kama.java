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

import com.epam.deltix.computations.finanalysis.util.KamaProcessor;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.DoubleStatefulFunctionBase;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;

import javax.naming.OperationNotSupportedException;

@Function("KAMA")
public class Kama extends DoubleStatefulFunctionBase {

    private KamaProcessor kama;

    @Init
    public void init(@Arg(defaultValue = "14") int period) {
        kama = new KamaProcessor(period);
    }

    @Compute
    public void compute(@BuiltInTimestampMs long timestamp, double v) {
        if (TimebaseTypes.isNull(v)) {
            return;
        }

        try {
            kama.add(v, timestamp);
            value = kama.value;
        } catch (OperationNotSupportedException ignored) {
        }
    }

}

