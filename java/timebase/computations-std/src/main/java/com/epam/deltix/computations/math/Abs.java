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
package com.epam.deltix.computations.math;

import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.FunctionsRepo;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.computations.api.util.FunctionsUtils;
import com.epam.deltix.computations.api.util.IntObjObjConsumer;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.collections.generated.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@FunctionsRepo
public final class Abs {

    @Function("ABS")
    public static float abs(float v) {
        return Math.abs(v);
    }

    @Function("ABS")
    public static double abs(double v) {
        return Math.abs(v);
    }

    @Function("ABS")
    @Decimal
    public static long absDecimal(@Decimal long v) {
        return v == Decimal64Utils.NULL ? Decimal64Utils.NULL : Decimal64Utils.absChecked(v);
    }

    @Function("ABS")
    public static byte abs(byte v) {
        return v == IntegerDataType.INT8_NULL ? IntegerDataType.INT8_NULL : (byte) Math.abs(v);
    }

    @Function("ABS")
    public static short abs(short v) {
        return v == IntegerDataType.INT16_NULL ? IntegerDataType.INT16_NULL : (short) Math.abs(v);
    }

    @Function("ABS")
    public static int abs(int v) {
        return v == IntegerDataType.INT32_NULL ? IntegerDataType.INT32_NULL : Math.abs(v);
    }

    @Function("ABS")
    public static long abs(long v) {
        return v == IntegerDataType.INT64_NULL ? IntegerDataType.INT64_NULL : Math.abs(v);
    }
    
    @Function("ABS")
    public static boolean abs(@Nullable ByteArrayList array, @Result @Nonnull ByteArrayList result) {
        return process(array, result, (i, source, target) -> target.add(abs(source.getByte(i))));
    }

    @Function("ABS")
    public static boolean abs(@Nullable ShortArrayList array, @Result @Nonnull ShortArrayList result) {
        return process(array, result, (i, source, target) -> target.add(abs(source.getShort(i))));
    }

    @Function("ABS")
    public static boolean abs(@Nullable IntegerArrayList array, @Result @Nonnull IntegerArrayList result) {
        return process(array, result, (i, source, target) -> target.add(abs(source.getInteger(i))));
    }

    @Function("ABS")
    public static boolean abs(@Nullable LongArrayList array, @Result @Nonnull LongArrayList result) {
        return process(array, result, (i, source, target) -> target.add(abs(source.getLong(i))));
    }

    @Function("ABS")
    public static boolean abs(@Nullable FloatArrayList array, @Result @Nonnull FloatArrayList result) {
        return process(array, result, (i, source, target) -> target.add(abs(source.getFloat(i))));
    }

    @Function("ABS")
    public static boolean abs(@Nullable DoubleArrayList array, @Result @Nonnull DoubleArrayList result) {
        return process(array, result, (i, source, target) -> target.add(abs(source.getDouble(i))));
    }

    @Function("ABS")
    public static boolean absDecimal(@Nullable @Decimal LongArrayList array, @Result @Nonnull @Decimal LongArrayList result) {
        return process(array, result, (i, source, target) -> target.add(absDecimal(source.getLong(i))));
    }
    
    private static <T extends List<?>> boolean process(@Nullable T array, @Nonnull T result, @Nonnull IntObjObjConsumer<T> consumer) {
        result.clear();

        if (FunctionsUtils.isNullOrEmpty(array)) {
            return false;
        }

        for (int i = 0; i < array.size(); i++) {
            consumer.consume(i, array, result);
        }
        return true;
    }

}
