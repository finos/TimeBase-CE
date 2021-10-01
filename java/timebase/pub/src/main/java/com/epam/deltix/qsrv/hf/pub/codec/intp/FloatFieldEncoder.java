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
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.util.text.CharSequenceParser;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
final class FloatFieldEncoder extends FieldEncoder {
    private final int       relativeIdx;
    private final int       baseIdx;

    private final boolean hasConstraint;
    private float min;
    private float max;

    FloatFieldEncoder (NonStaticFieldLayout f) {
        super (f);        
        relativeIdx = f.relativeTo == null ? -1 : f.relativeTo.ownBaseIndex;
        baseIdx = f.ownBaseIndex;

        // prepare for constraint validation
        min = max = Float.NaN;
        final Class<?> type = fieldType != null ? fieldType : float.class;
        final Number minBoxed = CodecUtils.getMinLimit(f.getType(), true, type);
        final Number maxBoxed = CodecUtils.getMaxLimit(f.getType(), true, type);
        if (minBoxed != null || maxBoxed != null) {
            hasConstraint = true;
            min = minBoxed != null ? minBoxed.floatValue() : -Float.MAX_VALUE;
            max = maxBoxed != null ? maxBoxed.floatValue() : Float.MAX_VALUE;
        } else
            hasConstraint = false;
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        setFloat (getter.getFloat(obj), ctxt);
    }

    void                    writeNull(EncodingContext ctxt) {
        setFloat(FloatDataType.IEEE32_NULL, ctxt);
    }

    @Override
    boolean                 isNull(CharSequence value) {
        return value == null || Float.isNaN(CharSequenceParser.parseFloat(value));
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        try {
            setFloat (CharSequenceParser.parseFloat (value), ctxt);
        } catch (IllegalArgumentException e) {
            // rethrow an exception with un-parsed string value
            final String msg = e.getMessage();
            if (msg != null && msg.startsWith(fieldDescription + " == "))
                throwConstraintViolationException(value);
            else
                throw e;
        }
    }

    void                    writeFloat (float value, EncodingContext ctxt) {
        ctxt.out.writeFloat (value);
    }
    
    @Override
    void                    setFloat (float value, EncodingContext ctxt) {
        validate(value);
        
        if (baseIdx >= 0)
            ctxt.floatBaseValues [baseIdx] = value;
        
        if (relativeIdx >= 0) {
            float               base = ctxt.floatBaseValues [relativeIdx];
            
            if (!Float.isNaN (base))
                value -= base;
        }
        
        writeFloat (value, ctxt);
    }
    
    @Override
    void                    setDouble (double value, EncodingContext ctxt) {
        setFloat ((float) value, ctxt);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return Float.isNaN(getter.getFloat(message));
    }

    private void validate(float v) {
        if (Float.isNaN(v)) {
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