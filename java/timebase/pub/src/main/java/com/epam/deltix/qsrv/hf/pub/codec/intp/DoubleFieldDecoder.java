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
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.FloatFieldOutOfRange;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.IllegalNullValue;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.ValidationError;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.lang.MathUtil;
import com.epam.deltix.util.lang.StringUtils;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class DoubleFieldDecoder extends FieldDecoder {
    private final int       relativeIdx;
    private final double    min;
    private final double    max;
    private final boolean   isDecimal;

    protected final int     baseIdx;
    
    DoubleFieldDecoder (NonStaticFieldLayout f) {
        super (f);
        relativeIdx = f.relativeTo == null ? -1 : f.relativeTo.ownBaseIndex;
        baseIdx = f.ownBaseIndex;
        
        FloatDataType fdt = (FloatDataType) f.getType ();
        isDecimal = fdt.getScale() == FloatDataType.SCALE_DECIMAL64;

        min = fdt.getMinNotNull ().doubleValue ();
        max = fdt.getMaxNotNull ().doubleValue (); 
    }

    @Override
    final int           compare (DecodingContext ctxt1, DecodingContext ctxt2) {
        final double v1 = getDouble(ctxt1);
        assert isNullable || !Double.isNaN(v1) : getNotNullableMsg();
        final double v2 = getDouble(ctxt2);
        assert isNullable || !Double.isNaN(v2) : getNotNullableMsg();
        final int result = CodecUtils.compareNulls(Double.isNaN(v1), Double.isNaN(v2));
        if (result < 2)
            return result;
        else
            return (MathUtil.compare(v1, v2));
    }

    @Override
    final protected void copy (DecodingContext ctxt, Object obj)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {

        if (fieldType == long.class) { // special case for decimals
            final long v = getLong(ctxt);
            assert isNullable || IntegerDataType.INT64_NULL != v : getNotNullableMsg();
            setter.setLong(obj, v);
        } else if (fieldType == Decimal64.class) { // special case for decimals
            final long v = getLong(ctxt);
            assert isNullable || IntegerDataType.INT64_NULL != v : getNotNullableMsg();
            setter.set(obj, Decimal64.fromUnderlying(v));
        } else {
            final double v = getDouble(ctxt);
            assert isNullable || !Double.isNaN(v) : getNotNullableMsg();
            setter.setDouble(obj, v);
        }
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        if (fieldType == long.class) { // special case for decimals
            setter.setLong(obj, IntegerDataType.INT64_NULL);
        } else if (fieldType == Decimal64.class) { // special case for decimals
            setter.set(obj, Decimal64.NULL);
        } else {
            setter.setDouble(obj, Double.NaN);
        }
    }

    @Override
    protected void setNull(Object obj, int idx) {
        if (fieldType == long.class) {
            Array.setLong(obj, idx, IntegerDataType.INT64_NULL);
        } else {
            Array.setDouble(obj, idx, Double.NaN);
        }
    }

    double              readDouble (DecodingContext ctxt) {
        return (isDecimal ? ctxt.in.readDecimal64() : ctxt.in.readDouble ());
    }

    @Override
    final double        getDouble (DecodingContext ctxt) {
        double              d = readDouble (ctxt);
        
        if (relativeIdx >= 0) {            
            double          base = ctxt.doubleBaseValues [relativeIdx];

            if (!Double.isNaN (base))
                d += base;
        }
        
        if (baseIdx >= 0)
            ctxt.doubleBaseValues [baseIdx] = d;
        
        return (d);
    }

    @Override
    long                getLong(DecodingContext ctx) {
        if (isDecimal)
            return ctx.in.readLong();

        return super.getLong(ctx);
    }

    @Override
    final float         getFloat (DecodingContext ctxt) {
        return (float) getDouble (ctxt);
    }

    @Override
    final String        getString (DecodingContext ctxt) {
        if (isDecimal) {
            long lv = getLong(ctxt);
            return lv != Decimal64Utils.NULL ? Decimal64Utils.toString(lv) : null;
        } else {
            double v = getDouble(ctxt);
            return isNull(v) ? null : StringUtils.toDecimalString(v);
        }
    }

    @Override
    void                skip (DecodingContext ctxt) {
        if (baseIdx >= 0)
            ctxt.doubleBaseValues[baseIdx] = readDouble(ctxt);
        else
            ctxt.in.skipBytes(8);
    }

    public boolean      isNull(DecodingContext ctxt) {
        return isNull(getDouble(ctxt));
    }

    @Override
    public boolean      isNull(double value) {
        return Double.isNaN(value);
    }

    @Override
    public boolean      isNull(long value) {
        if (isDecimal)
            return value == Decimal64Utils.NULL;

        return super.isNull(value);
    }
    
    @Override
    public ValidationError      validate (DecodingContext ctxt) {
        final int           offset = ctxt.in.getCurrentOffset ();        
        final double        v = getDouble (ctxt);
        
        if (isNull (v)) {
            if (!isNullable)
                return (new IllegalNullValue (offset, fieldInfo));        
        }
        else if (v < min || v > max)
            return (new FloatFieldOutOfRange (offset, fieldInfo, v, min, max));
        
        return (null);
    }
}