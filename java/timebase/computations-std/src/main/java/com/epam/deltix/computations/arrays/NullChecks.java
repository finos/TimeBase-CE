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
package com.epam.deltix.computations.arrays;

import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.FunctionsRepo;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.computations.api.annotations.Signature;
import com.epam.deltix.computations.api.util.FunctionsUtils;
import com.epam.deltix.computations.api.util.IntObjToBoolFunction;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.hf.pub.md.VarcharDataType;
import com.epam.deltix.util.annotations.Alphanumeric;
import com.epam.deltix.util.annotations.Bool;
import com.epam.deltix.util.annotations.TimeOfDay;
import com.epam.deltix.util.annotations.TimestampMs;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.collections.generated.DoubleArrayList;
import com.epam.deltix.util.collections.generated.FloatArrayList;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ShortArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@FunctionsRepo
public class NullChecks {

    @Function("ISNULL")
    public static boolean isNull(@Nullable ByteArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> arr.getByte(i) == IntegerDataType.INT8_NULL);
    }

    @Function("ISNULL")
    public static boolean isNull(@Nullable ShortArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> arr.getShort(i) == IntegerDataType.INT16_NULL);
    }

    @Function("ISNULL")
    public static boolean isNull(@Nullable IntegerArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> arr.getInteger(i) == IntegerDataType.INT32_NULL);
    }

    @Function("ISNULL")
    public static boolean isNull(@Nullable LongArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> arr.getLong(i) == IntegerDataType.INT64_NULL);
    }

    @Function("ISNULL")
    public static boolean isNull(@Nullable FloatArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> Float.isNaN(arr.getFloat(i)));
    }

    @Function("ISNULL")
    public static boolean isNull(@Nullable DoubleArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> Double.isNaN(arr.getDouble(i)));
    }

    @Function("ISNULL")
    public static boolean isNullDecimal(@Nullable @Decimal LongArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> Decimal64Utils.isNull(arr.getLong(i)));
    }

    @Function("ISNULL")
    public static boolean isNullBoolean(@Nullable ByteArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> arr.getByte(i) == BooleanDataType.NULL);
    }

    @Function("ISNULL")
    public static boolean isNullTimestamp(@Nullable @TimestampMs LongArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> arr.getLong(i) == DateTimeDataType.NULL);
    }

    @Function("ISNULL")
    public static boolean isNullTimestamp(@Nullable @TimeOfDay IntegerArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> arr.getInteger(i) == TimeOfDayDataType.NULL);
    }

    @Function("ISNULL")
    public static boolean isNullAlphanumeric(@Nullable @Alphanumeric LongArrayList array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> arr.getLong(i) == VarcharDataType.ALPHANUMERIC_NULL);
    }


    @Signature(id = "ISNULL", args = "ARRAY(VARCHAR?)?", returns = "ARRAY(BOOLEAN?)?")
    @Signature(id = "ISNULL", args = "ARRAY(OBJECT?)?", returns = "ARRAY(BOOLEAN?)?")
    public static boolean isNull(@Nullable ObjectArrayList<?> array, @Result @Bool @Nonnull ByteArrayList result) {
        return boolArray(array, result, (i, arr) -> arr.get(i) == null);
    }

    private static <T extends List<?>> boolean boolArray(@Nullable T array, @Bool @Nonnull ByteArrayList result,
                                                         IntObjToBoolFunction<T> function) {
        result.clear();

        if (FunctionsUtils.isNullOrEmpty(array)) {
            return false;
        }

        for (int i = 0; i < array.size(); i++) {
            result.set(i, FunctionsUtils.bpos(function.apply(i, array)));
        }
        return true;
    }

}