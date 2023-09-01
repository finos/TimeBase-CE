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
import com.epam.deltix.computations.api.util.FunctionsUtils;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.collections.generated.*;

import javax.annotation.Nullable;

import static com.epam.deltix.dfp.Decimal64Utils.*;

@FunctionsRepo
public class Avg {

    @Function("AVG")
    @Decimal
    public static long avg(@Nullable ByteArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return Decimal64Utils.NULL;
        @Decimal long result = Decimal64Utils.ZERO;
        @Decimal long count = Decimal64Utils.ONE;
        for (int i = 0; i < list.size(); i++) {
            if (list.getByte(i) != IntegerDataType.INT8_NULL) {
                result = add(result, divide(subtract(fromInt(list.getByte(i)), result), count));
                count = Decimal64Utils.add(count, Decimal64Utils.ONE);
            }
        }
        return result;
    }

    @Function("AVG")
    @Decimal
    public static long avg(@Nullable ShortArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return Decimal64Utils.NULL;
        @Decimal long result = Decimal64Utils.ZERO;
        @Decimal long count = Decimal64Utils.ONE;
        for (int i = 0; i < list.size(); i++) {
            if (list.getShort(i) != IntegerDataType.INT16_NULL) {
                result = add(result, divide(subtract(fromInt(list.getShort(i)), result), count));
                count = Decimal64Utils.add(count, Decimal64Utils.ONE);
            }
        }
        return result;
    }
    
    @Function("AVG")
    @Decimal
    public static long avg(@Nullable IntegerArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return Decimal64Utils.NULL;
        @Decimal long result = Decimal64Utils.ZERO;
        @Decimal long count = Decimal64Utils.ONE;
        for (int i = 0; i < list.size(); i++) {
            if (list.getInteger(i) != IntegerDataType.INT32_NULL) {
                result = add(result, divide(subtract(fromInt(list.getInteger(i)), result), count));
                count = Decimal64Utils.add(count, Decimal64Utils.ONE);
            }
        }
        return result;
    }

    @Function("AVG")
    @Decimal
    public static long avg(@Nullable LongArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return Decimal64Utils.NULL;
        @Decimal long result = Decimal64Utils.ZERO;
        @Decimal long count = Decimal64Utils.ONE;
        for (int i = 0; i < list.size(); i++) {
            if (list.getLong(i) != IntegerDataType.INT64_NULL) {
                result = add(result, divide(subtract(fromLong(list.getLong(i)), result), count));
                count = Decimal64Utils.add(count, Decimal64Utils.ONE);
            }
        }
        return result;
    }

    @Function("AVG")
    public static float avg(@Nullable FloatArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return FloatDataType.IEEE32_NULL;
        float result = 0;
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            if (!Float.isNaN(list.getFloat(i))) {
                result += (list.getFloat(i) - result) / ++count;
            }
        }
        return result;
    }

    @Function("AVG")
    public static double avg(@Nullable DoubleArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return FloatDataType.IEEE64_NULL;
        double result = 0;
        int count = 0;
        for (int i = 0; i < list.size(); i++) {
            if (!Double.isNaN(list.getDouble(i))) {
                result += (list.getDouble(i) - result) / ++count;
            }
        }
        return result;
    }

    @Function("AVG")
    @Decimal
    public static long avgDecimal(@Nullable @Decimal LongArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return Decimal64Utils.NULL;
        @Decimal long result = Decimal64Utils.ZERO;
        @Decimal long count = Decimal64Utils.ONE;
        for (int i = 0; i < list.size(); i++) {
            if (list.getLong(i) != Decimal64Utils.NULL) {
                result = add(result, divide(subtract(list.getLong(i), result), count));
                count = Decimal64Utils.add(count, Decimal64Utils.ONE);
            }
        }
        return result;
    }
    
}