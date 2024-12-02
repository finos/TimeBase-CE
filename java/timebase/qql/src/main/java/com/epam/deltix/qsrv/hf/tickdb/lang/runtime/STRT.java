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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.qsrv.util.json.DateFormatter;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.lang.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 */
public abstract class STRT {

    public static <T> void castToString(T value, StringBuilder result) {
        result.setLength(0);
        if (value != null) {
            result.append(value);
        }
    }

    public static void castBoolToString(byte value, StringBuilder result) {
        result.setLength(0);
        if (value != BooleanDataType.NULL) {
            result.append(value == 0 ? "false" : "true");
        }
    }

    public static void castToString(byte value, StringBuilder result) {
        result.setLength(0);
        if (value != IntegerDataType.INT8_NULL) {
            result.append(value);
        }
    }

    public static void castToString(short value, StringBuilder result) {
        result.setLength(0);
        if (value != IntegerDataType.INT16_NULL) {
            result.append(value);
        }
    }

    public static void castToString(int value, StringBuilder result) {
        result.setLength(0);
        if (value != IntegerDataType.INT32_NULL) {
            result.append(value);
        }
    }

    public static void castToString(long value, StringBuilder result) {
        result.setLength(0);
        if (value != IntegerDataType.INT64_NULL) {
            result.append(value);
        }
    }

    public static void castToString(float value, StringBuilder result) {
        result.setLength(0);
        if (!Float.isNaN(value)) {
            result.append(StringUtils.toDecimalString(value));
        }
    }

    public static void castToString(double value, StringBuilder result) {
        result.setLength(0);
        if (!Double.isNaN(value)) {
            result.append(StringUtils.toDecimalString(value));
        }
    }

    public static void castDecimalToString(long value, StringBuilder result) {
        result.setLength(0);
        if (!Decimal64Utils.isNull(value)) {
            Decimal64Utils.floatAppendTo(value, result);
        }
    }

    public static void castToString(char value, StringBuilder result) {
        result.setLength(0);
        if (value != CharDataType.NULL) {
            result.append(value);
        }
    }

    public static void castToString(CharSequence value, StringBuilder result) {
        result.setLength(0);
        if (value != null) {
            result.append(value);
        }
    }

    public static void castTimeOfDayToString(int value, StringBuilder result) {
        result.setLength(0);
        if (value != TimeOfDayDataType.NULL) {
            formatTimeofDayMillis(value, result);
        }
    }

    public static void castTimestampToString(long value, StringBuilder result, DateFormatter formatter) {
        result.setLength(0);
        if (value != DateTimeDataType.NULL) {
            formatter.toDateString(value, result);
        }
    }

    public static void castTimestampNsToString(long value, StringBuilder result, DateFormatter formatter) {
        result.setLength(0);
        if (value != DateTimeDataType.NULL) {
            formatter.toNanosDateString(value, result);
        }
    }

    public static void castBinaryToString(Binary value, StringBuilder result) {
        result.setLength(0);
        if (value != null && !value.isNull()) {
            result.append("[");
            byte[] bytes = value.bytes();
            int len = value.length();
            for (int i = 0; i < len; ++i) {
                if (i > 0) {
                    result.append(",");
                }
                result.append(bytes[i]);
            }
            result.append("]");
        }
    }

    /*
     * ARRAY TO STRING
     */

    private interface IntObj1Obj2Consumer<T1, T2> {
        void consume(int i, T1 obj1, T2 obj2);
    }

    public static void castBoolArrayToString(ByteArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            byte value = source.get(i);
            if (value != BooleanDataType.NULL) {
                target.append(value == 0 ? "false" : "true");
            } else {
                target.append("null");
            }
        });
    }

    public static void castArrayToString(ByteArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            byte value = source.get(i);
            if (value != IntegerDataType.INT8_NULL) {
                target.append(value);
            } else {
                target.append("null");
            }
        });
    }

    public static void castArrayToString(ShortArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            short value = source.get(i);
            if (value != IntegerDataType.INT16_NULL) {
                target.append(value);
            } else {
                target.append("null");
            }
        });
    }

    public static void castArrayToString(IntegerArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            int value = source.get(i);
            if (value != IntegerDataType.INT32_NULL) {
                target.append(value);
            } else {
                target.append("null");
            }
        });
    }

    public static void castArrayToString(LongArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            long value = source.get(i);
            if (value != IntegerDataType.INT64_NULL) {
                target.append(value);
            } else {
                target.append("null");
            }
        });
    }

    public static void castArrayToString(FloatArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            float value = source.get(i);
            if (!Float.isNaN(value)) {
                target.append(StringUtils.toDecimalString(value));
            } else {
                target.append("null");
            }
        });
    }

    public static void castArrayToString(DoubleArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            double value = source.get(i);
            if (!Double.isNaN(value)) {
                target.append(StringUtils.toDecimalString(value));
            } else {
                target.append("null");
            }
        });
    }

    public static void castDecimalArrayToString(@Decimal LongArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            long value = source.get(i);
            if (!Decimal64Utils.isNull(value)) {
                Decimal64Utils.floatAppendTo(value, target);
            } else {
                target.append("null");
            }
        });
    }

    public static void castArrayToString(CharacterArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            char value = source.get(i);
            if (value != CharDataType.NULL) {
                target.append(value);
            } else {
                target.append("null");
            }
        });
    }

    public static void castTimeOfDayArrayToString(IntegerArrayList array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            int value = source.get(i);
            if (value != TimeOfDayDataType.NULL) {
                target.append('"');
                formatTimeofDayMillis(value, target);
                target.append('"');
            } else {
                target.append("null");
            }
        });
    }

    public static void castTimestampArrayToString(LongArrayList array, StringBuilder result, DateFormatter formatter) {
        result.setLength(0);
        if (array != null) {
            result.append("[");
            for (int i = 0; i < array.size(); ++i) {
                if (i > 0) {
                    result.append(",");
                }

                long value = array.get(i);
                if (value != DateTimeDataType.NULL) {
                    result.append('"');
                    formatter.toDateString(value, result);
                    result.append('"');
                } else {
                    result.append("null");
                }
            }
            result.append("]");
        }
    }

    public static void castVarcharArrayToString(ObjectArrayList<CharSequence> array, StringBuilder result) {
        castArrayToString(array, result, (i, source, target) -> {
            CharSequence text = source.get(i);
            if (text != null) {
                target.append('"');
                for (int j = 0; j < text.length(); j++) {
                    char ch = text.charAt(j);
                    if (ch == '"')
                        target.append('\\');
                    target.append(ch);
                }
                target.append('"');
            } else {
                target.append("null");
            }
        });
    }

    public static <T extends List<?>> void castArrayToString(T array, StringBuilder result, IntObj1Obj2Consumer<T, StringBuilder> consumer) {
        result.setLength(0);
        if (array != null) {
            result.append("[");
            for (int i = 0; i < array.size(); ++i) {
                if (i > 0) {
                    result.append(",");
                }

                consumer.consume(i, array, result);
            }
            result.append("]");
        }
    }

    /*
     * ENUM TO STRING
     */

    public static void castEnumToString(long value, StringBuilder result, EnumClassDescriptor descriptor) {
        result.setLength(0);
        if (descriptor != null) {
            if (value != EnumDataType.NULL) {
                result.append(descriptor.longToString(value));
            }
        } else {
            result.append("<UNKNOWN_ENUM>");
        }
    }

    public static void castEnumArrayToString(ByteArrayList array, StringBuilder result, EnumClassDescriptor descriptor) {
        castArrayToString(array, result, (i, source, target) -> {
            byte value = source.get(i);
            if (value != EnumDataType.NULL) {
                target.append('"');
                target.append(descriptor.longToString(value));
                target.append('"');
            } else {
                target.append("null");
            }
        });
    }


    public static void castEnumArrayToString(ShortArrayList array, StringBuilder result, EnumClassDescriptor descriptor) {
        castArrayToString(array, result, (i, source, target) -> {
            short value = source.get(i);
            if (value != EnumDataType.NULL) {
                target.append('"');
                target.append(descriptor.longToString(value));
                target.append('"');
            } else {
                target.append("null");
            }
        });
    }

    public static void castEnumArrayToString(IntegerArrayList array, StringBuilder result, EnumClassDescriptor descriptor) {
        castEnumArrayToString(array, result, (i, source, target) -> {
            int value = source.get(i);
            if (value != EnumDataType.NULL) {
                target.append('"');
                target.append(descriptor.longToString(value));
                target.append('"');
            } else {
                target.append("null");
            }
        });
    }

    public static void castEnumArrayToString(LongArrayList array, StringBuilder result, EnumClassDescriptor descriptor) {
        castEnumArrayToString(array, result, (i, source, target) -> {
            long value = source.get(i);
            if (value != EnumDataType.NULL) {
                target.append('"');
                target.append(descriptor.longToString(value));
                target.append('"');
            } else {
                target.append("null");
            }
        });
    }

    public static <T extends List<?>> void castEnumArrayToString(T array, StringBuilder result, IntObj1Obj2Consumer<T, StringBuilder> consumer) {
        result.setLength(0);
        if (array != null) {
            result.append("[");
            for (int i = 0; i < array.size(); ++i) {
                if (i > 0) {
                    result.append(",");
                }

                consumer.consume(i, array, result);
            }
            result.append("]");
        } else {
            result.append("<UNKNOWN_ENUM_ARRAY>");
        }
    }

    /*
     * OBJECT TO STRING
     */

    public static void castObjectToString(Object value, StringBuilder result, RecordClassDescriptor... descriptors) {
        result.setLength(0);
        if (descriptors.length > 0) {
            result.append("<object of ");
            for (int i = 0; i < descriptors.length; ++i) {
                if (i > 0) {
                    result.append(",");
                }
                result.append(descriptors[i]);
            }
            result.append(">");
        } else {
            result.append(value.toString());
        }
    }

    public static void castObjectArrayToString(Object value, StringBuilder result, RecordClassDescriptor... descriptors) {
        result.setLength(0);
        if (descriptors.length > 0) {
            result.append("<array of object<");
            for (int i = 0; i < descriptors.length; ++i) {
                if (i > 0) {
                    result.append(",");
                }
                result.append(descriptors[i]);
            }
            result.append(">>");
        } else {
            result.append(value.toString());
        }
    }

    /*
     * UTILS
     */

    public static void formatTimeofDayMillis(int timeOfDay, StringBuilder sb) {
        int value = timeOfDay;
        int ms = value % 1000;
        value /= 1000;
        int s = value % 60;
        value /= 60;
        int m = value % 60;
        value /= 60;
        if (value >= 24)
            throw new IllegalArgumentException("TOD too large: " + timeOfDay);

        int h = value;
        boolean hasMillis = ms != 0;
        boolean hasSec = s != 0 || hasMillis;

        sb.append(String.format("%02d:%02d", h, m));
        if (hasSec) {
            sb.append(String.format(":%02d", s));
        }
        if (hasMillis) {
            sb.append(String.format(".%03d", ms));
        }
    }

    // parse functions

    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("[yyyyMMdd][yyyy-MM-dd][yyyy-DDD]['T'[HHmmss][HHmm][HH:mm:ss][HH:mm][.SSSSSSSSS][.SSSSSS][.SSS][.SS][.S]][OOOO][O][z][XXXXX][XXXX]['['VV']']")
        .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
        .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
        .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
        .toFormatter()
        .withZone(ZoneId.of("UTC"));

    public static long parseDateTime(DateTimeFormatter dtf, CharSequence value) {
        if (value == null) {
            return TimeConstants.TIMESTAMP_UNKNOWN;
        }

        try {
            Instant instant = parseToInstant(dtf, value);
            return instant.toEpochMilli();
        } catch (Throwable t) {
            return TimeConstants.TIMESTAMP_UNKNOWN;
        }
    }

    public static long parseDateTime(CharSequence value) {
        return parseDateTime(DATE_TIME_FORMATTER, value);
    }

    public static long parseDateTimeNs(DateTimeFormatter dtf, CharSequence value) {
        if (value == null) {
            return TimeConstants.TIMESTAMP_UNKNOWN;
        }

        try {
            Instant instant = parseToInstant(dtf, value);
            return instant.getEpochSecond() * 1_000_000_000 + instant.getNano();
        } catch (Throwable t) {
            return TimeConstants.TIMESTAMP_UNKNOWN;
        }
    }

    public static long parseDateTimeNs(CharSequence value) {
        return parseDateTimeNs(DATE_TIME_FORMATTER, value);
    }

    public static Instant parseToInstant(DateTimeFormatter dtf, CharSequence value) {
        return dtf.parse(value, Instant::from);
    }

    public static byte parseBoolean(CharSequence sc) {
        if ("true".contentEquals(sc)) {
            return 1;
        } else if ("false".contentEquals(sc)) {
            return 0;
        }

        return -1;
    }

    public static byte parseByte(CharSequence cs) {
        try {
            int result = Integer.parseInt(cs, 0, cs.length(), 10);
            if ((result < Byte.MIN_VALUE) || (result > Byte.MAX_VALUE)) {
                return Byte.MIN_VALUE;
            } else {
                return (byte) result;
            }
        } catch (Throwable t) {
            return Byte.MIN_VALUE;
        }
    }

    public static short parseShort(CharSequence cs) {
        try {
            int result = Integer.parseInt(cs, 0, cs.length(), 10);
            if ((result < Short.MIN_VALUE) || (result > Short.MAX_VALUE)) {
                return Short.MIN_VALUE;
            } else {
                return (short) result;
            }
        } catch (Throwable t) {
            return Short.MIN_VALUE;
        }
    }

    public static int parseInt(CharSequence cs) {
        try {
            return Integer.parseInt(cs, 0, cs.length(), 10);
        } catch (Throwable t) {
            return Integer.MIN_VALUE;
        }
    }

    public static long parseLong(CharSequence cs) {
        try {
            return Long.parseLong(cs, 0, cs.length(), 10);
        } catch (Throwable t) {
            return Long.MIN_VALUE;
        }
    }

    public static float parseFloat(CharSequence cs) {
        try {
            return TextUtil.parseFloat(cs);
        } catch (Throwable t) {
            return Float.NaN;
        }
    }

    public static double parseDouble(CharSequence cs) {
        try {
            return TextUtil.parseDouble(cs);
        } catch (Throwable t) {
            return Float.NaN;
        }
    }

    public static long parseDecimal(CharSequence cs) {
        try {
            return Decimal64Utils.parse(cs);
        } catch (Throwable t) {
            return Decimal64Utils.NULL;
        }
    }
}
