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

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class FixedSizeIntegerFieldDecoder extends IntegerFieldDecoder {
    private final int   size;
    private final int   index;

    FixedSizeIntegerFieldDecoder (NonStaticFieldLayout f, int size) {
        super (f);
        this.size = size;
        this.index = IntegerDataType.getIndex(size);
    }

    @Override
    public long     getLong (DecodingContext ctxt) {
        switch (size) {
            case 1:         return (ctxt.in.readByte ());
            case 2:         return (ctxt.in.readShort ());
            case 4:         return (ctxt.in.readInt ());
            case 6:         return (ctxt.in.readLong48 ());
            case 8:         return (ctxt.in.readLong ());
            default:        throw new RuntimeException ("Illegal size: " + size);
        }
    }
    
    @Override
    public void     skip (DecodingContext ctxt) {
        ctxt.in.skipBytes (size);
    }

    @Override
    public boolean isNull(long value) {
        return value == IntegerDataType.NULLS [index];
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        final long value = IntegerDataType.NULLS[index];
        setValue(obj, value);
    }

    @Override
    protected void setNull(Object obj, int idx) {
        Class<?> fc = obj.getClass();
        long value = IntegerDataType.NULLS[index];
        if (fc == int[].class)
            Array.setInt(obj, idx, (int) value);
        else if (fc == long[].class)
            Array.setLong(obj, idx, value);
        else if (fc == short[].class)
            Array.setShort(obj, idx, (short) value);
        else if (fc == byte[].class)
            Array.setByte(obj, idx, (byte) value);
        else
            throw new IllegalArgumentException(fc.isArray() ? fc.getComponentType() + "[]" : fc.getName());
    }
}
