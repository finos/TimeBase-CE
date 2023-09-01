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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.util.lang.MathUtil;
import com.epam.deltix.util.time.GMT;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class DateTimeFieldDecoder extends FieldDecoder {
    DateTimeFieldDecoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    public int      compare (DecodingContext ctxt1, DecodingContext ctxt2) {
        return (MathUtil.compare (getLong (ctxt1), getLong (ctxt2)));
    }

    @Override
    final protected void copy (DecodingContext ctxt, Object obj)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        long        t = getLong (ctxt);
        assert isNullable || !isNull(t) : getNotNullableMsg();

        setter.setLong(obj, t);
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setter.setLong(obj, Long.MIN_VALUE);
    }

    public boolean isNull(DecodingContext ctxt) {
        return isNull(ctxt.in.readLong());
    }

    @Override
    public long     getLong (DecodingContext ctxt) {
        return (ctxt.in.readLong ());
    }

    @Override
    public String   getString (DecodingContext ctxt) {
        final long v = getLong(ctxt);
        assert isNullable || !isNull(v) : getNotNullableMsg();
        return (isNull(v) ? null : GMT.formatDateTimeMillis(v));
    }

    @Override
    public void     skip (DecodingContext ctxt) {
        ctxt.in.skipBytes (8);
    }

    @Override
    public boolean isNull(long value) {
        return value == DateTimeDataType.NULL;
    }
}