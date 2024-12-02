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
package com.epam.deltix.computations.math;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.FunctionsRepo;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.util.collections.generated.DoubleArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionsRepo
public final class Ceil {

    @Function("CEIL")
    public static double ceil(double v) {
        return Math.ceil(v);
    }

    @Function("CEIL")
    @Decimal
    public static long ceilDecimal(@Decimal long v) {
        return v == Decimal64Utils.NULL ? Decimal64Utils.NULL : Decimal64Utils.fromDouble(Math.ceil(Decimal64Utils.toDouble(v)));
    }

    @Function("CEIL")
    public static boolean ceil(@Nullable DoubleArrayList array, @Result @Nonnull DoubleArrayList result) {
        return Util.process(array, result, (i, source, target) -> target.add(ceil(source.getDouble(i))));
    }

    @Function("CEIL")
    public static boolean ceilDecimal(@Nullable @Decimal LongArrayList array, @Result @Nonnull @Decimal LongArrayList result) {
        return Util.process(array, result, (i, source, target) -> target.add(ceilDecimal(source.getLong(i))));
    }

}