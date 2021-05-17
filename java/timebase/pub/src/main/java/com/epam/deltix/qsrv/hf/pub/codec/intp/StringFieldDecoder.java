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

import com.epam.deltix.qsrv.hf.codec.BinaryUtils;
import com.epam.deltix.qsrv.hf.pub.codec.CharSequenceDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.lang.Util;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class StringFieldDecoder extends FieldDecoder {
    private final boolean       isCharSequence;

    StringFieldDecoder (NonStaticFieldLayout f) {
        super (f);
        isCharSequence = fieldType == CharSequence.class;
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
        final CharSequence v = isCharSequence ? ctxt.readCharSequence(ctxt.in) : getString(ctxt);
        assert isNullable || v != null : getNotNullableMsg();
        if (((NonStaticFieldLayout) (this).fieldInfo).getFieldType() == ByteArrayList.class)
            setter.set(obj, BinaryUtils.assign((ByteArrayList) setter.get(obj), v));
        else
            setter.set(obj, v);
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setter.set(obj, null);
    }

    public boolean isNull(DecodingContext ctxt) {
        return getCharSequence (ctxt) == null;
    }

    CharSequence    getCharSequence (DecodingContext ctxt) {
        return (ctxt.in.readCharSequence ());
    }

    @Override
    public String   getString (DecodingContext ctxt) {
        CharSequence    cs = getCharSequence (ctxt);

        return (cs == null ? null : cs.toString ());
    }

    @Override
    public void     skip (DecodingContext ctxt) {
        ctxt.in.readCharSequence ();
    }
}
