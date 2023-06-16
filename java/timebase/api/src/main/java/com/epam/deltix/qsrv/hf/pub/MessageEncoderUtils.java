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

import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;

import java.util.AbstractList;
import java.util.ArrayList;

/**
 *
 */
final public class MessageEncoderUtils {

    public static void writeField(Object value, UnboundEncoder uenc) {
        final DataType type = uenc.getField().getType();
        writeField(value, type, uenc);
    }

    public static void writeField(Object value, DataType type, WritableValue uenc) {
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

            ArrayList values = (ArrayList) value;
            ClassDataType cType = (ClassDataType) type;

            RecordClassDescriptor rcd = null;
            int index = 0;

            if (values.get(0) instanceof RecordClassDescriptor) {
                rcd = (RecordClassDescriptor) values.get(0);
                index = 1;
            } else if (cType.isFixed()) {
                rcd = cType.getFixedDescriptor();
            }

            assert rcd != null;

            writeObject(values, uenc.getFieldEncoder(rcd), index);
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

    public static void writeObject(ArrayList values, UnboundEncoder encoder, int index) {
        final int len = values.size();

        for (int i = index; i < len && encoder.nextField(); i++) {
            Object v = values.get(i);
            if (v != null)
                writeField(v, encoder);
        }
    }
}