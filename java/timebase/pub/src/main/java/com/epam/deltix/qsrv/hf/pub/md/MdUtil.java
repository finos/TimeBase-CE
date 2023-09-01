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
package com.epam.deltix.qsrv.hf.pub.md;

import net.jcip.annotations.GuardedBy;

/**
 *
 */
public abstract class MdUtil {

    public static int getSize(Class<?> type) {
        if (type == byte.class )
            return 1;
        else if (type == short.class)
            return 2;
        else if (type == int.class)
            return 4;
        else if (type == long.class)
            return 8;
        else
            throw new IllegalArgumentException("Unexpected type " + type);
    }

    public static boolean isIntegerType(Class<?> type) {
        return type == byte.class || type == short.class || type == int.class || type == long.class ;
    }

    public static boolean isStringType(Class<?> type) {
        return type == CharSequence.class || type == String.class;
    }

    public static void validateIntegerRange(Class<?> type, Number value) {
        final long min, max;
        if (type == byte.class) {
            min = Byte.MIN_VALUE;
            max = Byte.MAX_VALUE;
        } else if (type == short.class) {
            min = Short.MIN_VALUE;
            max = Short.MAX_VALUE;
        } else if (type == int.class) {
            min = Integer.MIN_VALUE;
            max = Integer.MAX_VALUE;
        } else
            return;

        final long v = value.longValue();
        if (v < min || v > max)
            throw new IllegalArgumentException(formatMsg(type, value));
    }

    public static void validateFloatRange(Class<?> type, Number value) {
        if (type == float.class) {
            final double v = value.doubleValue();
            if (v < -Float.MAX_VALUE || v > Float.MAX_VALUE)
                throw new IllegalArgumentException(formatMsg(type,  value));
        }
    }

    @GuardedBy("itself")
    private static final StringBuilder sb = new StringBuilder();

    private static String formatMsg(Class<?> type,  Number value) {
        synchronized (sb) {
            sb.setLength(0);
            sb.append("Cannot store static value ").append(value).append(" to ");
            sb.append(type.getName());

            return sb.toString();
        }
    }

    public static Object getIntValue(Class<?> fieldType, long n) {
        if (fieldType == byte.class)
            return (new Byte((byte) n));

        if (fieldType == short.class)
            return (new Short((short) n));

        if (fieldType == int.class)
            return (new Integer((int) n));

        if (fieldType == long.class)
            return (new Long(n));

        throw new RuntimeException(fieldType.toString());
    }

    public static Object getNullValue(DataType dataType, Class<?> fieldType) {
        final Class<?> baseType =  fieldType;

        if (dataType instanceof BooleanDataType)
            return (BooleanDataType.NULL);


        if (dataType instanceof IntegerDataType) {
            long n = ((IntegerDataType) dataType).getNullValue();
            return (getIntValue(fieldType, n));
        }

        if (dataType instanceof VarcharDataType) {
            if (baseType == long.class)
                return (VarcharDataType.ALPHANUMERIC_NULL);
            return (null);
        }

        if (dataType instanceof DateTimeDataType)
            return (DateTimeDataType.NULL);

        if (dataType instanceof CharDataType)
            return (CharDataType.NULL);

        if (dataType instanceof TimeOfDayDataType)
            return (TimeOfDayDataType.NULL);

        if (dataType instanceof FloatDataType) {
            if (fieldType == float.class)
                return (Float.NaN);

            return (Double.NaN);
        }

        if (dataType instanceof EnumDataType) {
            if (fieldType.isPrimitive())
                return (getIntValue(fieldType, EnumDataType.NULL));

            return (null);
        }

        if (dataType instanceof BinaryDataType)
            return (null);

        if (dataType instanceof ArrayDataType)
            return null;

        if (dataType instanceof ClassDataType)
            return null;

        throw new RuntimeException(dataType + " --> " + fieldType);
    }
}