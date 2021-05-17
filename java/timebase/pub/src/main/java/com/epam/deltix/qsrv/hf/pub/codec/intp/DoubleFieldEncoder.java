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

import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.text.CharSequenceParser;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class DoubleFieldEncoder extends FieldEncoder {
    private final int       relativeIdx;
    private final int       baseIdx;

    private final boolean   hasConstraint;
    private double          min;
    private double          max;
    private final boolean   isDecimal;

    DoubleFieldEncoder(NonStaticFieldLayout f) {
        super(f);
        relativeIdx = f.relativeTo == null ? -1 : f.relativeTo.ownBaseIndex;
        baseIdx = f.ownBaseIndex;

        isDecimal = ((FloatDataType) f.getType ()).getScale() == FloatDataType.SCALE_DECIMAL64;

        // prepare for constraint validation
        min = max = Double.NaN;
        final Class<?> type = fieldType != null ? fieldType : double.class;
        final Number minBoxed = CodecUtils.getMinLimit(f.getType(), true, type);
        final Number maxBoxed = CodecUtils.getMaxLimit(f.getType(), true, type);
        if (minBoxed != null || maxBoxed != null) {
            hasConstraint = true;
            min = minBoxed != null ? minBoxed.doubleValue() : -Double.MAX_VALUE;
            max = maxBoxed != null ? maxBoxed.doubleValue() : Double.MAX_VALUE;
        } else
            hasConstraint = false;
    }

    @Override
    final protected void    copy (Object obj, EncodingContext ctx)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        if (fieldType == long.class) { // special case for decimals
            setLong(getter.getLong(obj), ctx);
        } else if (fieldType == Decimal64.class) {
            Decimal64 v = (Decimal64) getter.get(obj);
            setLong(Decimal64.toUnderlying(v), ctx);
        } else {
            setDouble(getter.getDouble(obj), ctx);
        }
    }

    void                    writeNull(EncodingContext ctxt) {
        setDouble(FloatDataType.IEEE64_NULL, ctxt);
    }

    @Override
    boolean                 isNull(CharSequence value) {
        return value == null || Double.isNaN(CharSequenceParser.parseDouble(value));
    }

    @Override
    protected boolean isNull(long value) {
        if (isDecimal)
            return value == Decimal64Utils.NULL;

        return super.isNull(value);
    }

    @Override
    final void              setString (CharSequence value, EncodingContext ctxt) {
        try {
            setDouble(CharSequenceParser.parseDouble(value), ctxt);
        } catch (IllegalArgumentException e) {
            // rethrow an exception with un-parsed string value
            final String msg = e.getMessage();
            if (msg != null && msg.startsWith(fieldDescription + " == "))
                throwConstraintViolationException(value);
            else
                throw e;
        }
    }

    @Override
    void                        setLong(long value, EncodingContext ctx) {
        if (isDecimal)
            ctx.out.writeLong(value);
        else
            super.setLong(value, ctx);
    }

    void                        writeDouble (double value, EncodingContext ctxt) {
        if (isDecimal)
            ctxt.out.writeDecimal64(value);
        else
            ctxt.out.writeDouble (value);
    }

    @Override
    final void                  setDouble (double value, EncodingContext ctxt) {
        validate(value);

        if (baseIdx >= 0)
            ctxt.doubleBaseValues [baseIdx] = value;

        if (relativeIdx >= 0) {
            double              base = ctxt.doubleBaseValues [relativeIdx];

            if (!Double.isNaN (base))
                value -= base;
        }

        writeDouble (value, ctxt);
    }

    @Override
    final void                  setFloat (float value, EncodingContext ctxt) {
        setDouble (value, ctxt);
    }

    @Override
    protected boolean           isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        if (fieldType == long.class) // special case for decimals
            return IntegerDataType.INT64_NULL == getter.getLong(message);
        else if (fieldType == Decimal64.class)
            return getter.get(message) == null;

        return Double.isNaN(getter.getDouble(message));
    }

    private void                validate(double v) {
        if (Double.isNaN(v)) {
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
