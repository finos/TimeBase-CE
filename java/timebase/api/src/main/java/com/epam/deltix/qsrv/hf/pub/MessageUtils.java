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
package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageUtils {

    public static final String    OBJECT_CLASS_NAME        = "objectClassName";

    @SuppressWarnings("unchecked")
    private static Object readArray(ArrayDataType type, ReadableValue udec) throws NullValueException {
        final int len = udec.getArrayLength();
        final DataType underlineType = type.getElementDataType();
        final boolean isNullableBool = (underlineType instanceof BooleanDataType) && underlineType.isNullable();
        final boolean isByte = (underlineType instanceof IntegerDataType) && (((IntegerDataType) underlineType).getNativeTypeSize() == 1);
        final boolean isShort = (underlineType instanceof IntegerDataType) && (((IntegerDataType) underlineType).getNativeTypeSize() == 2);
        try {
            //final AbstractList a = (AbstractList) RecordLayout.getNativeType(type).newInstance();
            //setArrayLength(a, len);

            RecordLayout.getNativeType(type).newInstance();
            final AbstractList a = new ArrayList<Object>();
            for (int i = 0; i < len; i++)
                a.add(null);

            for (int i = 0; i < len; i++) {
                Object value;
                try {
                    final ReadableValue rv = udec.nextReadableElement();
                    value = readField(underlineType, rv);
                } catch (NullValueException e) {
                    value = null;
                }

                if (value == null && !isNullableBool) {
                    if (a instanceof ArrayList)
                        a.set(i, null);
                    else
                        return readArrayUncasted(i, a, underlineType, udec);
                } else {
                    if (isNullableBool) {
                        Boolean b = (Boolean) value;
                        a.set(i, (byte) (b == null ? -1 : b ? 1 : 0));
                    } else if (isByte) {
                        a.set(i, ((Integer) value).byteValue());
                    } else if (isShort) {
                        a.set(i, ((Integer) value).shortValue());
                    } else
                        a.set(i, value);
                }
            }
            return a;

        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object readArrayUncasted(int idx, final AbstractList a, DataType underlineType , ReadableValue udec) {
        @SuppressWarnings("unchecked")
        final ArrayList<Object> r = new ArrayList<Object>(a);
        r.set(idx, null);
        for (int i = idx + 1; i < a.size(); i++) {
            try {
                final ReadableValue rv = udec.nextReadableElement();
                r.set(i, readField(underlineType, rv));
            } catch (NullValueException e) {
                r.set(i, null);
            }
        }

        return r;
    }

    public static Object readField(UnboundDecoder udec) {
        final DataType type = udec.getField().getType();
        return readField(type, udec);
    }

    public static Object readField(DataType type, ReadableValue udec) {
        try {
            if (type instanceof IntegerDataType) {
                if (((IntegerDataType) type).getNativeTypeSize() >= 6)
                    return udec.getLong();
                else
                    return udec.getInt();
            } else if (type instanceof FloatDataType)
                if (((FloatDataType) type).isFloat())
                    return udec.getFloat();
                else
                    return udec.getDouble();
            else if (type instanceof CharDataType)
                return udec.getChar();
            else if (type instanceof EnumDataType || type instanceof VarcharDataType)
                return udec.getString();
            else if (type instanceof BooleanDataType)
                return udec.getBoolean();
            else if (type instanceof DateTimeDataType)
                return udec.getLong();
            else if (type instanceof TimeOfDayDataType)
                return udec.getInt();
            else if (type instanceof ArrayDataType)
                return readArray((ArrayDataType) type, udec);
            else if (type instanceof ClassDataType)
                 return readObjectValues(udec);
            else if (type instanceof BinaryDataType) {
                try {
                    final int size = udec.getBinaryLength();
                    final byte[] bin = new byte[size];
                    udec.getBinary(0, size, bin, 0);
                    return bin;
                } catch (NullValueException e) {
                    return null;
                }
            } else
                throw new RuntimeException("Unrecognized dataType: " + type);
        } catch (NullValueException e) {
            return null;
        }
    }

    public static Map<String, Object> readObjectValues(ReadableValue udec) throws NullValueException {
        try {
            final UnboundDecoder decoder = udec.getFieldDecoder();
            Map<String, Object> valuesMap = new LinkedHashMap<>();

            if (decoder.getClassInfo() != null)
                valuesMap.put(OBJECT_CLASS_NAME, decoder.getClassInfo().getDescriptor().getName());

            // dump field/value pairs
            while (decoder.nextField()) {
                NonStaticFieldInfo field = decoder.getField();
                Object value = readField(decoder);
                if (field.getType() instanceof DateTimeDataType)
                    value = formatDate (field.getType(), value);
                valuesMap.put(field.getName(), value);
            }
            return valuesMap;
        } catch (NullValueException e) {
            return null;
        }
    }

    private static Object formatDate (DataType dataType, Object value){
        if (value instanceof Long)
            return dataType.toString(value);

        return value;
    }

    public static void      writeField(Object value, UnboundEncoder uenc) {
        final DataType type = uenc.getField().getType();
        writeField(value, type, uenc);
    }

    public static void      writeField(Object value, DataType type, WritableValue uenc) {
        if (value == null)
            uenc.writeNull();
        else if (value instanceof CharSequence)
            uenc.writeString((CharSequence) value);
        else if (type instanceof IntegerDataType)
            uenc.writeLong(((Number) value).longValue());
        else if (type instanceof FloatDataType) {
            if (type.getEncoding().equals(FloatDataType.ENCODING_FIXED_FLOAT))
                uenc.writeFloat(((Number) value).floatValue());
            else
                uenc.writeDouble(((Number) value).doubleValue());
        }
        else if (type instanceof EnumDataType || type instanceof VarcharDataType || type instanceof CharDataType) {
            if (value instanceof Integer)
                uenc.writeLong((Integer) value);
            else if (value instanceof Character)
                uenc.writeChar((Character) value);
            else if (type instanceof EnumDataType && value.getClass().isEnum())
                uenc.writeLong(((Enum) value).ordinal());
            else
                throw new IllegalArgumentException(value.getClass().getName());
        }
        else if (type instanceof BooleanDataType) {
            if (value instanceof Boolean)
                uenc.writeBoolean((Boolean) value);
            else
                uenc.writeBoolean((Byte) value == BooleanDataType.TRUE);
        }
        else if (type instanceof DateTimeDataType)
            uenc.writeLong((Long) value);
        else if (type instanceof TimeOfDayDataType)
            uenc.writeInt((Integer) value);
        else if (type instanceof ArrayDataType)
            writeArray(value, (ArrayDataType) type, uenc);
        else if (type instanceof ClassDataType) {

            RecordClassDescriptor[] rcds = ((ClassDataType) type).getDescriptors();

            Map values = (Map) value;
            Object name = values.get(OBJECT_CLASS_NAME);
            RecordClassDescriptor current = null;

            if (name != null) {
                for (RecordClassDescriptor descriptor : rcds) {
                    if (descriptor.getName().equals(name))
                        current = descriptor;
                }
            } else {
                current = rcds[0];
            }

            assert current != null;

            writeObject(values, uenc.getFieldEncoder(current));
        }
        else if (type instanceof BinaryDataType) {
            byte[] bin = (byte[]) value;
            uenc.writeBinary(bin, 0, bin.length);
        } else
            throw new RuntimeException("Unrecognized dataType: " + type);
    }


    @SuppressWarnings("unchecked")
    public static void writeArray(Object value, ArrayDataType type, WritableValue uenc) {
        final AbstractList a = (AbstractList) value;
        final int len = a.size();
        uenc.setArrayLength(len);
        final DataType underlineType = type.getElementDataType();

        for (int i = 0; i < len; i++) {
            final Object v = a.get(i);
            final WritableValue rv = uenc.nextWritableElement();

            if (v != null || type.getElementDataType().isNullable())
                writeField(v, underlineType, rv);
        }
    }

    public static void writeObject(Map values, UnboundEncoder encoder) {

        while (encoder.nextField()) {
            Object v = values.get(encoder.getField().getName());
            if (v != null)
                writeField(v, encoder);
        }
    }

}