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
public final class Floor {

    @Function("FLOOR")
    public static double floor(double v) {
        return Math.floor(v);
    }

    @Function("FLOOR")
    @Decimal
    public static long floorDecimal(@Decimal long v) {
        return v == Decimal64Utils.NULL ? Decimal64Utils.NULL : Decimal64Utils.fromDouble(Math.floor(Decimal64Utils.toDouble(v)));
    }

    @Function("FLOOR")
    public static boolean floor(@Nullable DoubleArrayList array, @Result @Nonnull DoubleArrayList result) {
        return Util.process(array, result, (i, source, target) -> target.add(floor(source.getDouble(i))));
    }

    @Function("FLOOR")
    public static boolean floorDecimal(@Nullable @Decimal LongArrayList array, @Result @Nonnull @Decimal LongArrayList result) {
        return Util.process(array, result, (i, source, target) -> target.add(floorDecimal(source.getLong(i))));
    }

}