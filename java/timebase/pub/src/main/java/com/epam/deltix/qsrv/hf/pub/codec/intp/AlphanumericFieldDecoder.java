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

import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.lang.Util;

import java.lang.reflect.InvocationTargetException;

/**
 * User: BazylevD
 * Date: Oct 21, 2009
 */
class AlphanumericFieldDecoder extends FieldDecoder {
    private final AlphanumericCodec codec;
    AlphanumericFieldDecoder (NonStaticFieldLayout f,  int length) {
        super (f);
        codec = new AlphanumericCodec(length);
    }

    @Override
    public int      compare (DecodingContext ctxt1, DecodingContext ctxt2) {
        CharSequence        s1 = getCharSequence (ctxt1);
        assert isNullable || s1 != null : getNotNullableMsg();
        CharSequence        s2 = getCharSequence (ctxt2);
        assert isNullable || s2 != null : getNotNullableMsg();
        return (Util.compare (s1, s2, true));
    }

    @Override
    final protected void copy (DecodingContext ctxt, Object obj)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (fieldType == int.class) {
            final int v = codec.readInt(ctxt.in);
            assert isNullable || v != IntegerDataType.INT32_NULL : getNotNullableMsg();
            setter.setInt(obj, v);
        } else if (fieldType == long.class) {
            final long v = codec.readLong(ctxt.in);
            assert isNullable || v != IntegerDataType.INT64_NULL : getNotNullableMsg();
            setter.setLong(obj, v);
        } else if (fieldType == String.class) {
            final String v = getString(ctxt);
            assert isNullable || v != null : getNotNullableMsg();
            setter.set(obj, v);
        } else if (fieldType == CharSequence.class) {
            final CharSequence v = getCharSequence(ctxt);
            assert isNullable || v != null : getNotNullableMsg();
            setter.set(obj, v);
        } else
            throw new IllegalArgumentException("Type " + fieldType + " is not supported.");
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        if (fieldType == int.class) {
            setter.setInt(obj, Integer.MIN_VALUE);
        } else if (fieldType == long.class) {
            setter.setLong(obj, Long.MIN_VALUE);
        } else if (fieldType == String.class || fieldType == CharSequence.class) {
            setter.set(obj, null);
        } else
            throw new IllegalArgumentException("Type " + fieldType + " is not supported.");
    }

    public boolean isNull(DecodingContext ctxt) {
        return getCharSequence (ctxt) == null;
    }

    @Override
    public boolean isNull(long value) {
        if (fieldType == int.class)
            return value == IntegerDataType.INT32_NULL;
        else if (fieldType == long.class || fieldType == null)
            return value == IntegerDataType.INT64_NULL;
        else
            throw new IllegalArgumentException("Type " + fieldType + " is not supported.");
    }

    private CharSequence getCharSequence(DecodingContext ctxt) {
        return codec.readCharSequence(ctxt.in);
    }

    @Override
    long getLong(DecodingContext ctxt) {
        final long v = codec.readLong(ctxt.in);
        assert isNullable || v != IntegerDataType.INT64_NULL : getNotNullableMsg();
        return v;
    }

    @Override
    public String   getString (DecodingContext ctxt) {
        final CharSequence cs = getCharSequence(ctxt);
        return (cs == null ? null : cs.toString());
    }

    @Override
    public void     skip (DecodingContext ctxt) {
        codec.skip(ctxt.in);
    }
}
