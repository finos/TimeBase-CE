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

import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.util.text.CharSequenceParser;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
abstract class IntegerFieldEncoder extends FieldEncoder {
    private final boolean hasConstraint;
    private long min;
    private long max;

    IntegerFieldEncoder (NonStaticFieldLayout f) {
        super (f);

        // prepare for constraint validation
        min = max = 0;
        if (!(this instanceof TimeOfDayFieldEncoder)) {
            final Class<?> type = fieldType != null ? fieldType : long.class;
            final Number minBoxed = CodecUtils.getMinLimit(f.getType(), true, type);
            final Number maxBoxed = CodecUtils.getMaxLimit(f.getType(), true, type);
            if (minBoxed != null || maxBoxed != null) {
                hasConstraint = true;
                min = minBoxed != null ? minBoxed.longValue() : Long.MIN_VALUE;
                max = maxBoxed != null ? maxBoxed.longValue() : Long.MAX_VALUE;
            } else
                hasConstraint = false;
        } else
            hasConstraint = false;
    }

    abstract void setLongImpl(long value, EncodingContext ctxt);

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        setLong(getValue(obj), ctxt);
    }

    @Override
    boolean                 isNull(CharSequence value) {
        return value == null || isNull(CharSequenceParser.parseLong(value));
    }

    @Override
    void                    setByte (byte value, EncodingContext ctxt) {
        setLong(value, ctxt);
    }

    @Override
    void                    setInt (int value, EncodingContext ctxt) {
        setLong(value, ctxt);
    }

    @Override
    void                    setShort (short value, EncodingContext ctxt) {
        setLong(value, ctxt);
    }

    @Override
    void           setLong (long value, EncodingContext ctxt) {
        validate(value);
        setLongImpl(value, ctxt);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return isNull(getValue(message));
    }

    private long getValue(Object obj) throws IllegalAccessException, InvocationTargetException {
        if (fieldType == int.class)
            return getter.getInt(obj);
        else if (fieldType == long.class)
            return getter.getLong(obj);
        else if (fieldType == short.class)
            return getter.getShort(obj);
        else if (fieldType == byte.class)
            return getter.getByte(obj);
        else
            throw new RuntimeException(fieldType.getName());
    }

    private void validate(long v) {
        if (isNull(v)) {
            if (!isNullable)
                throwNotNullableException();
            else
                return;
        }

        // range check
        if (hasConstraint) {
            if (v < min || v > max)
                throwConstraintViolationException(v);
        }
    }
}
