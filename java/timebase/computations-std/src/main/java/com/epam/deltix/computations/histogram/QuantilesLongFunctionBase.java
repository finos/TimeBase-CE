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
package com.epam.deltix.computations.histogram;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.computations.api.annotations.*;
import com.epam.deltix.computations.api.generated.arrays.LongArrayStatefulFunctionBase;
import com.epam.deltix.qsrv.hf.pub.md.TimebaseTypes;
import com.epam.deltix.util.collections.generated.LongArrayList;

public abstract class QuantilesLongFunctionBase extends LongArrayStatefulFunctionBase {

    protected QuantilesLong histogram;

    protected double[] quantiles;

    protected boolean reset = false;

    public QuantilesLongFunctionBase() {
    }

    @Compute
    public void compute(long v) {
        if (TimebaseTypes.isNull(v) || v < 0) {
            return;
        }

        histogram.add(v);
        fillBuffer();

        if (value == null) {
            value = buffer;
        }
    }

    @Reset
    @Override
    public void reset() {
        if (reset) {
            value = null;
            resetBuffer();
        }
    }

    protected void init(@Decimal LongArrayList q) {
        this.quantiles = Utils.decimalToDoubleList(q).toDoubleArray();
        resetBuffer();
    }

    protected void fillBuffer() {
        for (int i = 0; i < quantiles.length; ++i) {
            buffer.set(i, histogram.calcQuantile(quantiles[i]));
        }
    }

    protected void resetBuffer() {
        this.buffer.setSize(this.quantiles.length);
        for (int i = 0; i < buffer.size(); ++i) {
            buffer.set(i, 0);
        }
    }

}
