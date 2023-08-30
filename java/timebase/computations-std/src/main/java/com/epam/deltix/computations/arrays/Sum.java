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
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.collections.generated.*;

import static com.epam.deltix.computations.api.util.FunctionsUtils.isNullOrEmpty;

@FunctionsRepo
public class Sum {

    @Function("SUM")
    public static int sum(ByteArrayList list) {
        if (isNullOrEmpty(list)) {
            return IntegerDataType.INT16_NULL;
        }
        int result = 0;
        for (int i = 0; i < list.size(); i++) {
            result += list.getByte(i);
        }
        return result;
    }

    @Function("SUM")
    public static int sum(ShortArrayList list) {
        if (isNullOrEmpty(list)) {
            return IntegerDataType.INT32_NULL;
        }
        int result = 0;
        for (int i = 0; i < list.size(); i++) {
            result += list.getShort(i);
        }
        return result;
    }

    @Function("SUM")
    public static long sum(IntegerArrayList list) {
        if (isNullOrEmpty(list)) {
            return IntegerDataType.INT64_NULL;
        }
        long result = 0;
        for (int i = 0; i < list.size(); i++) {
            result += list.getInteger(i);
        }
        return result;
    }

    @Function("SUM")
    @Decimal
    public static long sum(LongArrayList list) {
        if (isNullOrEmpty(list)) {
            return Decimal64Utils.NULL;
        }
        long result = Decimal64Utils.ZERO;
        for (int i = 0; i < list.size(); i++) {
            result = Decimal64Utils.add(result, Decimal64Utils.fromLong(list.getLong(i)));
        }
        return result;
    }

    @Function("SUM")
    public static float sum(FloatArrayList list) {
        if (isNullOrEmpty(list)) {
            return FloatDataType.IEEE32_NULL;
        }
        float result = 0;
        for (int i = 0; i < list.size(); i++) {
            result += list.getFloat(i);
        }
        return result;
    }

    @Function("SUM")
    public static double sum(DoubleArrayList list) {
        if (isNullOrEmpty(list)) {
            return FloatDataType.IEEE64_NULL;
        }
        double result = 0;
        for (int i = 0; i < list.size(); i++) {
            result += list.getDouble(i);
        }
        return result;
    }

    @Function("SUM")
    @Decimal
    public static long sumDecimal(@Decimal LongArrayList list) {
        if (isNullOrEmpty(list)) {
            return Decimal64Utils.NULL;
        }
        long result = Decimal64Utils.ZERO;
        for (int i = 0; i < list.size(); i++) {
            result = Decimal64Utils.add(result, list.getLong(i));
        }
        return result;
    }
}