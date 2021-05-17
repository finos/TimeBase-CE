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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.codec.EnumAnalyzer;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.EnumClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.MdUtil;
import com.epam.deltix.util.collections.generated.LongToObjectHashMap;
import com.epam.deltix.util.lang.MathUtil;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class EnumFieldDecoder extends FieldDecoder {
    private final int                           size;
    private final EnumClassDescriptor           ed;
    private final LongToObjectHashMap<Enum> javaConstants;

    EnumFieldDecoder (NonStaticFieldLayout f) {
        super (f);

        ed = ((EnumDataType) f.getType()).descriptor;
        size = ed.computeStorageSize();

        if (f.getFieldType() != null) {
            final EnumAnalyzer ea = new EnumAnalyzer();
            boolean valid = ea.analyze(f.getFieldType(), ed, false);
            if (valid)
                throw new IllegalArgumentException("Class " + fieldType.getName() + " must contains all schema values for decoding!");
            javaConstants = ea.getEnumValues();
        } else {
            javaConstants = null;
        }
    }

    @Override
    public int      compare (DecodingContext ctxt1, DecodingContext ctxt2) {
        final long v1 = getLong(ctxt1);
        assert isNullable || v1 != EnumDataType.NULL : getNotNullableMsg();
        final long v2 = getLong(ctxt2);
        assert isNullable || v2 != EnumDataType.NULL : getNotNullableMsg();
        return (MathUtil.compare (v1, v2));
    }

    public Object         getValue(long value) {
        assert isNullable || value != EnumDataType.NULL : getNotNullableMsg();

        Object      enumValue;

        if (javaConstants != null) {                 // Java Enum or remapped .NET enum
            if (value == EnumDataType.NULL_CODE)
                enumValue = null;
            else {
                if (!javaConstants.containsKey(value))
                    throw new IllegalArgumentException("value is out of range " + value);
                enumValue = javaConstants.get(value, null);
                if (enumValue == null)
                    throw new IllegalArgumentException("value is out of range " + value);
            }
            return enumValue;
        } else if (MdUtil.isIntegerType(fieldType))
            return value;
        else if (MdUtil.isStringType(fieldType))
            return  (value == EnumDataType.NULL) ? null : ed.longToString(value);
        else
            throw new UnsupportedOperationException("Not supported operation with IKVM enum.");

    }

    @Override
    final protected void copy (DecodingContext ctxt, Object obj)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        long        longval = getLong (ctxt);
        assert isNullable || longval != EnumDataType.NULL : getNotNullableMsg();
        Object      enumValue;

        if (javaConstants != null) {                 // Java Enum or remapped .NET enum
            if (longval == EnumDataType.NULL_CODE)
                enumValue = null;
            else {
                if (!javaConstants.containsKey(longval))//(longval < 0 || longval > javaConstants.size() - 1)
                    throw new IllegalArgumentException("value is out of range " + longval);
                enumValue = javaConstants.get(longval, null);
                if (enumValue == null)
                    throw new IllegalArgumentException("value is out of range " + longval);
            }
        } else if (MdUtil.isIntegerType(fieldType)) {
            if (fieldType == byte.class)
                setter.setByte(obj, (byte) longval);
            else if (fieldType == short.class)
                setter.setShort(obj, (short) longval);
            else if (fieldType == int.class)
                setter.setInt(obj, (int) longval);
            else if (fieldType == long.class)
                setter.setLong(obj, longval);
            else
                throw new IllegalStateException(fieldType.getName());
            return;
        } else if (MdUtil.isStringType(fieldType))
            enumValue = (longval == EnumDataType.NULL) ? null : ed.longToString(longval);
        else    // Assume IKVM enum
            throw new UnsupportedOperationException("Not supported operation with IKVM enum.");

        setter.set(obj, enumValue);
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setter.set(obj, null);
    }

    @Override
    public long     getLong (DecodingContext ctxt) {
        switch (size) {
            case 1:         return (ctxt.in.readByte ());
            case 2:         return (ctxt.in.readShort ());
            case 4:         return (ctxt.in.readInt ());
            case 8:         return (ctxt.in.readLong ());
            default:        throw new RuntimeException ();
        }
    }

    @Override
    public String   getString (DecodingContext ctxt) {
        final long v = getLong(ctxt);
        return (v == EnumDataType.NULL) ? null : ed.longToString(v);
    }

    @Override
    public void     skip (DecodingContext ctxt) {
        ctxt.in.skipBytes (size);
    }

    public boolean isNull(DecodingContext ctxt) {
        return isNull (getLong(ctxt));
    }

    @Override
    public boolean isNull (long value) {
        return value == EnumDataType.NULL;
    }        
}
