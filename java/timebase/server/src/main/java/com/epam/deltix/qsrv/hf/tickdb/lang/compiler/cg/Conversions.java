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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.computations.api.util.IntObjToIntFunction;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.collections.generated.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static com.epam.deltix.computations.api.util.FunctionsUtils.isNullOrEmpty;

@Deprecated
public class Conversions {
    // INT32

//    @Function("INT32")
    public static int int32(byte v) {
        return v == IntegerDataType.INT8_NULL ? IntegerDataType.INT32_NULL : (int) v;
    }

//    @Function("INT32")
    public static int int32(short v) {
        return v == IntegerDataType.INT16_NULL ? IntegerDataType.INT32_NULL : (int) v;
    }

//    @Function("INT32")
    public static int int32(long v) {
        return v == IntegerDataType.INT64_NULL ? IntegerDataType.INT32_NULL : (int) v;
    }

//    @Function("INT32")
    public static int int32(float v) {
        return Float.isNaN(v) ? IntegerDataType.INT32_NULL : (int) v;
    }

//    @Function("INT32")
    public static int int32(double v) {
        return Double.isNaN(v) ? IntegerDataType.INT32_NULL : (int) v;
    }

//    @Function("INT32")
    public static int int32Decimal(@Decimal long v) {
        return v == Decimal64Utils.NULL ? IntegerDataType.INT32_NULL : (int) Decimal64Utils.toInt(v);
    }

    // ARRAY(INT32)

//    @Function("INT32")
    public static boolean int32(@Nullable ByteArrayList list, @Nonnull @Result IntegerArrayList result) {
        return cast(list, result, (i, l) -> int32(l.getByte(i)));
    }

//    @Function("INT32")
    public static boolean int32(@Nullable ShortArrayList list, @Nonnull @Result IntegerArrayList result) {
        return cast(list, result, (i, l) -> int32(l.getShort(i)));
    }

//    @Function("INT32")
    public static boolean int32(@Nullable LongArrayList list, @Nonnull @Result IntegerArrayList result) {
        return cast(list, result, (i, l) -> int32(l.getLong(i)));
    }

//    @Function("INT32")
    public static boolean int32(@Nullable FloatArrayList list, @Nonnull @Result IntegerArrayList result) {
        return cast(list, result, (i, l) -> int32(l.getFloat(i)));
    }

//    @Function("INT32")
    public static boolean int32(@Nullable DoubleArrayList list, @Nonnull @Result IntegerArrayList result) {
        return cast(list, result, (i, l) -> int32(l.getDouble(i)));
    }

//    @Function("INT32")
    public static boolean int32Decimal(@Nullable @Decimal LongArrayList list, @Nonnull @Result IntegerArrayList result) {
        return cast(list, result, (i, l) -> int32Decimal(l.getLong(i)));
    }

    private static <T extends List<?>> boolean cast(@Nullable T list, @Nonnull @Result IntegerArrayList result,
                                                    @Nonnull IntObjToIntFunction<T> function) {
        result.clear();

        if (isNullOrEmpty(list)) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            result.add(function.apply(i, list));
        }
        return true;
    }
}