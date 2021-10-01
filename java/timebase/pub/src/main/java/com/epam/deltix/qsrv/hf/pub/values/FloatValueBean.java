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
package com.epam.deltix.qsrv.hf.pub.values;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *  Holds a float value.
 */
public final class FloatValueBean extends ValueBean {
    private static final long serialVersionUID = 1L;

    private float           value;
    private final float     min;
    private final float     max;
    private final boolean   isNullable;

    private static float    nvl (Number n, float def) {
        return (n == null ? def : n.floatValue ());
    }

    public FloatValueBean (FloatDataType type) {
        super (type);
        isNullable = type.isNullable ();
        min = nvl (type.getMin (), Float.MIN_VALUE);
        max = nvl (type.getMax (), Float.MAX_VALUE);
        
        if (min > max)
            throw new IllegalArgumentException ("min > max");

        value =
            isNullable ?
                Float.NaN :
            min <= 0 && 0 <= max ?
                0 :
                min;   // Set to something legal.
    }

    public float        getRaw () {
        return (value);
    }

    @Override
    public byte getByte() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public short getShort() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public float        getFloat () throws NullValueException {
        if (Float.isNaN (value))
            throw NullValueException.INSTANCE;

        return (value);
    }

    @Override
    public double       getDouble () throws NullValueException {
        return ((double) getFloat ());
    }

    @Override
    public void         writeFloat (float value) {
        if (Float.isNaN (value)) {
            if (!isNullable)
                throw new IllegalArgumentException ("NULL");
        }
        else {
            if (value < min)
                throw new IllegalArgumentException (value + " < " + min);

            if (value > max)
                throw new IllegalArgumentException (value + " > " + max);
        }

        this.value = value;
    }

    @Override
    public void         writeDouble (double value) {
        writeFloat ((float) value);
    }

    @Override
    public String       getString () throws NullValueException {
        return (String.valueOf (getFloat ()));
    }

    @Override
    public boolean      isNull () {
        return (Float.isNaN (value));
    }

    @Override
    public void         writeNull () {
        writeFloat (Float.NaN);
    }

    @Override
    public void         writeString (CharSequence s) {
        writeFloat (DataType.parseFloat (s.toString ()));
    }
    
    @Override
    protected Object getBoxedValue() {
        return (isNull () ? null : value);
    }
}