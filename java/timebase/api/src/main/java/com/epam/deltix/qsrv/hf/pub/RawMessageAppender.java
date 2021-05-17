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
package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.InterpretingCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;

/**
 * Base class for implementing different appenders, that consume {@link RawMessage}.
 */
public interface RawMessageAppender<T extends Appendable> {
    ObjectToObjectHashMap<String, UnboundDecoder> DECODERS = new ObjectToObjectHashMap<>();

    boolean appendTimestamp(final long timestamp, final T appendable);

    boolean appendNanoTime(final long nanoTime, final T appendable);

    boolean appendSymbol(final CharSequence symbol, final T appendable);

    //boolean appendInstrumentType(final InstrumentType type, final T appendable);

    boolean appendType(final RecordClassDescriptor type, final T appendable);

    boolean append(final String name, final long value, final T appendable);

    boolean append(final String name, final int value, final T appendable);

    boolean append(final String name, final double value, final T appendable);

    boolean append(final String name, final float value, final T appendable);

    boolean append(final String name, final boolean value, final T appendable);

    boolean append(final String name, final String value, final T appendable);

    boolean append(final String name, final char value, final T appendable);

    default boolean appendField(final String name, final IntegerDataType intType, final ReadableValue decoder, final T appendable) {
        if (intType.getNativeTypeSize() >= 6) {
            long value = decoder.getLong();
            return append(name, value, appendable);
        } else {
            int value = decoder.getInt();
            return append(name, value, appendable);
        }
    }

    default boolean appendField(final String name, final FloatDataType floatType, final ReadableValue decoder, final T appendable) {
        if (floatType.isFloat()) {
            float value = decoder.getFloat();
            return append(name, value, appendable);
        } else if (floatType.isDecimal64()) {
            long value = decoder.getLong();
            return append(name, value, appendable);
        } else {
            double value = decoder.getDouble();
            return append(name, value, appendable);
        }
    }

    default boolean appendField(final String name, final BooleanDataType booleanType, final ReadableValue decoder, final T appendable) {
        boolean value = decoder.getBoolean();
        return append(name, value, appendable);
    }

    default boolean appendField(final String name, final DateTimeDataType dateTimeType, final ReadableValue decoder, final T appendable) {
        long value = decoder.getLong();
        return append(name, value, appendable);
    }

    default boolean appendField(final String name, final TimeOfDayDataType timeOfDayType, final ReadableValue decoder, final T appendable) {
        int value = decoder.getInt();
        return append(name, value, appendable);
    }

    default boolean appendField(final String name, final VarcharDataType varcharType, final ReadableValue decoder, final T appendable) {
        String value = decoder.getString();
        return append(name, value, appendable);
    }

    default boolean appendField(final String name, final CharDataType charType, final ReadableValue decoder, final T appendable) {
        char value = decoder.getChar();
        return append(name, value, appendable);
    }

    default boolean appendField(final String name, final EnumDataType enumType, final ReadableValue decoder, final T appendable) {
        long ordinal = decoder.getLong();
        String value = enumType.descriptor.longToString(ordinal);
        return append(name, value, appendable);
    }

    default boolean appendField(final String name, final ClassDataType classType, final ReadableValue decoder, final T appendable) {
        // toDo
        return false;
    }

    default boolean appendField(final String name, final ArrayDataType arrayType, final ReadableValue decoder, final T appendable) {
        // toDo
        return false;
    }

    default boolean appendField(final NonStaticFieldInfo fieldInfo, final ReadableValue decoder, final T appendable) {
        DataType type = fieldInfo.getType();
        if (type instanceof ClassDataType || type instanceof ArrayDataType) {
            return appendComplex(fieldInfo, decoder, appendable);
        } else {
            return appendPrimitive(fieldInfo, decoder, appendable);
        }
    }

    default boolean appendPrimitive(final NonStaticFieldInfo fieldInfo, final ReadableValue decoder, final T appendable) {
        DataType type = fieldInfo.getType();
        String name = fieldInfo.getName();
        if (type instanceof IntegerDataType) {
            IntegerDataType intType = (IntegerDataType) type;
            return appendField(name, intType, decoder, appendable);
        } else if (type instanceof FloatDataType) {
            FloatDataType floatType = (FloatDataType) type;
            return appendField(name, floatType, decoder, appendable);
        } else if (type instanceof BooleanDataType) {
            BooleanDataType booleanType = (BooleanDataType) type;
            return appendField(name, booleanType, decoder, appendable);
        } else if (type instanceof DateTimeDataType) {
            DateTimeDataType dateTimeType = (DateTimeDataType) type;
            return appendField(name, dateTimeType, decoder, appendable);
        } else if (type instanceof TimeOfDayDataType) {
            TimeOfDayDataType timeOfDayType = (TimeOfDayDataType) type;
            return appendField(name, timeOfDayType, decoder, appendable);
        } else if (type instanceof VarcharDataType) {
            VarcharDataType varcharType = (VarcharDataType) type;
            return appendField(name, varcharType, decoder, appendable);
        } else if (type instanceof CharDataType) {
            CharDataType charType = (CharDataType) type;
            return appendField(name, charType, decoder, appendable);
        } else if (type instanceof EnumDataType) {
            EnumDataType enumType = (EnumDataType) type;
            return appendField(name, enumType, decoder, appendable);
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported type %s.", type.getClass().getName()));
        }
    }

    default boolean appendComplex(final NonStaticFieldInfo fieldInfo, final ReadableValue decoder, final T appendable) {
        DataType type = fieldInfo.getType();
        String name = fieldInfo.getName();
        if (type instanceof ClassDataType) {
            ClassDataType classType = (ClassDataType) type;
            return appendField(name, classType, decoder, appendable);
        } else if (type instanceof ArrayDataType) {
            ArrayDataType arrayType = (ArrayDataType) type;
            return appendField(name, arrayType, decoder, appendable);
        } else {
            throw new UnsupportedOperationException(String.format("Unsupported type %s.", type.getClass().getName()));
        }
    }

    default boolean appendHeader(final RawMessage raw, final T appendable) {
        appendTimestamp(raw.getTimeStampMs(), appendable);
        appendNanoTime(raw.getNanoTime(), appendable);
        appendSymbol(raw.getSymbol(), appendable);
        //appendInstrumentType(raw.getInstrumentType(), appendable);
        appendType(raw.type, appendable);
        return true;
    }

    default boolean append(final RawMessage raw, final T appendable) {
        final UnboundDecoder decoder = getDecoder(raw.type);

        appendHeader(raw, appendable);

        while (decoder.nextField()) {
            final NonStaticFieldInfo fieldInfo = decoder.getField();
            appendField(fieldInfo, decoder, appendable);
        }

        return true;
    }

    static UnboundDecoder getDecoder(final RecordClassDescriptor type) {
        final String guid = type.getGuid();
        UnboundDecoder decoder;
        synchronized (DECODERS) {
            decoder = DECODERS.get(guid, null);
            if (decoder == null) {
                decoder = InterpretingCodecMetaFactory.INSTANCE.createFixedUnboundDecoderFactory(type).create();
                DECODERS.put(guid, decoder);
            }
        }
        return decoder;
    }
}
