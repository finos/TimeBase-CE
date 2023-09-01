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
package com.epam.deltix.computations.math;

import com.epam.deltix.computations.api.annotations.Function;
import com.epam.deltix.computations.api.annotations.FunctionsRepo;
import com.epam.deltix.computations.api.annotations.Result;
import com.epam.deltix.computations.api.util.FunctionsUtils;
import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.util.annotations.Bool;
import com.epam.deltix.util.annotations.TimeOfDay;
import com.epam.deltix.util.annotations.TimestampMs;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.collections.generated.CharacterArrayList;
import com.epam.deltix.util.collections.generated.DoubleArrayList;
import com.epam.deltix.util.collections.generated.FloatArrayList;
import com.epam.deltix.util.collections.generated.IntegerArrayList;
import com.epam.deltix.util.collections.generated.LongArrayList;
import com.epam.deltix.util.collections.generated.ShortArrayList;
import com.epam.deltix.util.lang.Util;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionsRepo
public final class Min {

    @Function("MIN")
    @Bool
    public static byte minBoolean(@Bool byte v1, @Bool byte v2) {
        if (v1 == BooleanDataType.NULL)
            return v2;
        else if (v2 == BooleanDataType.NULL)
            return v1;
        else
            return v1 < v2 ? v1: v2;
    }

    @Function("MIN")
    public static char min(char v1, char v2) {
        if (v1 == CharDataType.NULL)
            return v2;
        else if (v2 == CharDataType.NULL)
            return v1;
        else
            return v1 < v2 ? v1 : v2;
    }

    @Function("MIN")
    @Decimal
    public static long minDecimal(@Decimal long v1, @Decimal long v2) {
        if (Decimal64Utils.isNull(v1)) {
            return v2;
        } else if (Decimal64Utils.isNull(v2)) {
            return v1;
        } else {
            return Decimal64Utils.min(v1, v2);
        }
    }

    @Function("MIN")
    public static float min(float v1, float v2) {
        if (Float.isNaN(v1))
            return v2;
        else if (Float.isNaN(v2))
            return v1;
        else
            return Math.min(v1, v2);
    }

    @Function("MIN")
    public static double min(double v1, double v2) {
        if (Double.isNaN(v1))
            return v2;
        else if (Double.isNaN(v2))
            return v1;
        else
            return Math.min(v1, v2);
    }

    @Function("MIN")
    public static byte min(byte v1, byte v2) {
        if (v1 == IntegerDataType.INT8_NULL)
            return v2;
        else if (v2 == IntegerDataType.INT8_NULL)
            return v1;
        else
            return v1 < v2 ? v1: v2;
    }

    @Function("MIN")
    public static short min(short v1, short v2) {
        if (v1 == IntegerDataType.INT16_NULL)
            return v2;
        else if (v2 == IntegerDataType.INT16_NULL)
            return v1;
        else
            return v1 < v2 ? v1: v2;
    }

    @Function("MIN")
    public static int min(int v1, int v2) {
        if (v1 == IntegerDataType.INT32_NULL)
            return v2;
        else if (v2 == IntegerDataType.INT32_NULL)
            return v1;
        else
            return Math.min(v1, v2);
    }

    @Function("MIN")
    public static long min(long v1, long v2) {
        if (v1 == IntegerDataType.INT64_NULL)
            return v2;
        else if (v2 == IntegerDataType.INT64_NULL)
            return v1;
        else
            return Math.min(v1, v2);
    }

    @Function("MIN")
    @TimeOfDay
    public static int minTimeOfDay(@TimeOfDay int v1, @TimeOfDay int v2) {
        if (v1 == TimeOfDayDataType.NULL)
            return v2;
        else if (v2 == TimeOfDayDataType.NULL)
            return v1;
        else
            return Math.min(v1, v2);
    }

    @Function("MIN")
    @TimestampMs
    public static long minTimestamp(@TimestampMs long v1, @TimestampMs long v2) {
        if (v1 == DateTimeDataType.NULL)
            return v2;
        else if (v2 == DateTimeDataType.NULL)
            return v1;
        else
            return Math.min(v1, v2);
    }

    @Function("MIN")
    @Bool
    public static byte minBoolean(@Nullable @Bool ByteArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return BooleanDataType.NULL;
        byte value = BooleanDataType.NULL;
        for (int i = 0; i < list.size(); i++) {
            if (value == BooleanDataType.NULL || list.getByte(i) < value)
                value = list.getByte(i);
        }
        return value;
    }

    @Function("MIN")
    public static char min(@Nullable CharacterArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return CharDataType.NULL;
        char value = CharDataType.NULL;
        for (int i = 0; i < list.size(); i++) {
            if (value == CharDataType.NULL || list.getCharacter(i) < value)
                value = list.getCharacter(i);
        }
        return value;
    }

    @Function("MIN")
    public static byte min(@Nullable ByteArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return IntegerDataType.INT8_NULL;
        byte value = IntegerDataType.INT8_NULL;
        for (int i = 0; i < list.size(); i++) {
            if (value == IntegerDataType.INT8_NULL || list.getByte(i) < value)
                value = list.getByte(i);
        }
        return value;
    }

    @Function("MIN")
    public static short min(@Nullable ShortArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return IntegerDataType.INT16_NULL;
        short value = IntegerDataType.INT16_NULL;
        for (int i = 0; i < list.size(); i++) {
            if (value == IntegerDataType.INT16_NULL || list.getShort(i) < value)
                value = list.getShort(i);
        }
        return value;
    }

    @Function("MIN")
    public static int min(@Nullable IntegerArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return IntegerDataType.INT32_NULL;
        int value = IntegerDataType.INT32_NULL;
        for (int i = 0; i < list.size(); i++) {
            if (value == IntegerDataType.INT32_NULL || list.getInteger(i) < value)
                value = list.getInteger(i);
        }
        return value;
    }

    @Function("MIN")
    public static long min(@Nullable LongArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return IntegerDataType.INT64_NULL;
        long value = IntegerDataType.INT64_NULL;
        for (int i = 0; i < list.size(); i++) {
            if (value == IntegerDataType.INT64_NULL || list.getLong(i) < value)
                value = list.getLong(i);
        }
        return value;
    }

    @Function("MIN")
    public static float min(@Nullable FloatArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return FloatDataType.IEEE32_NULL;
        float value = FloatDataType.IEEE32_NULL;
        for (int i = 0; i < list.size(); i++) {
            if (Float.isNaN(value) || list.getFloat(i) < value)
                value = list.getFloat(i);
        }
        return value;
    }

    @Function("MIN")
    public static double min(@Nullable DoubleArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return FloatDataType.IEEE64_NULL;
        double value = FloatDataType.IEEE64_NULL;
        for (int i = 0; i < list.size(); i++) {
            if (Double.isNaN(value) || list.getDouble(i) < value)
                value = list.getDouble(i);
        }
        return value;
    }

    @Function("MIN")
    @Decimal
    public static long minDecimalArray(@Nullable @Decimal LongArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return Decimal64Utils.NULL;
        long value = Decimal64Utils.NULL;
        for (int i = 0; i < list.size(); i++) {
            if (value == Decimal64Utils.NULL || Decimal64Utils.isLess(list.getLong(i), value))
                value = list.getLong(i);
        }
        return value;
    }

    @Function("MIN")
    @TimestampMs
    public static long minTimestampArray(@Nullable @TimestampMs LongArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return DateTimeDataType.NULL;
        long value = DateTimeDataType.NULL;
        for (int i = 0; i < list.size(); i++) {
            if (value == DateTimeDataType.NULL || list.getLong(i) < value)
                value = list.getLong(i);
        }
        return value;
    }

    @Function("MIN")
    @TimeOfDay
    public static int minTimeOfDayArray(@Nullable @TimeOfDay IntegerArrayList list) {
        if (FunctionsUtils.isNullOrEmpty(list))
            return TimeOfDayDataType.NULL;
        int value = TimeOfDayDataType.NULL;
        for (int i = 0; i < list.size(); i++) {
            if (value == TimeOfDayDataType.NULL || list.getInteger(i) < value)
                value = list.getInteger(i);
        }
        return value;
    }

    @Function("MIN")
    public static boolean min(@Nullable CharSequence cs1, @Nullable CharSequence cs2, @Nonnull @Result StringBuilder sb) {
        sb.setLength(0);
        if (cs1 == null && cs2 == null) {
            return false;
        } else if (cs1 == null) {
            sb.append(cs2);
        } else if (cs2 == null) {
            sb.append(cs1);
        } else {
            sb.append(Util.compare(cs1, cs2, false) < 0 ? cs1: cs2);
        }
        return true;
    }

}