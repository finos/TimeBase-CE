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
 *  Holds an INTEGER value.
 */
public final class IntegerValueBean extends ValueBean {
    private static final long serialVersionUID = 1L;

    private long            value;
    private final long      nullValue;
    private final long      min;
    private final long      max;
    private final boolean   isNullable;

    public IntegerValueBean (IntegerDataType type) {
        super (type);
        nullValue = type.getNullValue ();
        isNullable = type.isNullable ();
        Number []   range = type.getRange ();
        min = range [0].longValue ();
        max = range [1].longValue ();

        if (min > max)
            throw new IllegalArgumentException ("min > max");

        value =
            isNullable ?
                nullValue :
            min <= 0 && 0 <= max ?
                0 :
                min;   // Set to something legal.
    }

    public long         getRaw () {
        return (value);
    }

    @Override
    public long         getLong () throws NullValueException {
        if (value == nullValue)
            throw NullValueException.INSTANCE;

        return (value);
    }

    @Override
    public int          getInt () throws NullValueException {
        long                lv = getLong ();
        
        if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE)
            throw new IllegalStateException (lv + " cannot be converted to int");

        return ((int) lv);
    }

    @Override
    public short getShort() throws NullValueException {
        long lv = getLong();

        if (lv < Short.MIN_VALUE || lv > Short.MAX_VALUE)
            throw new IllegalStateException(lv + " cannot be converted to short");

        return ((short) lv);
    }

    @Override
    public byte getByte() throws NullValueException {
        long lv = getLong();

        if (lv < Byte.MIN_VALUE || lv > Byte.MAX_VALUE)
            throw new IllegalStateException(lv + " cannot be converted to byte");

        return ((byte) lv);
    }

    @Override
    public void         writeLong (long value) {
        if (value == nullValue) {
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
    public void         writeInt (int value) {
        writeLong (value);
    }
    
    @Override
    public String       getString () throws NullValueException {
        return (String.valueOf (getLong ()));
    }

    @Override
    public boolean      isNull () {
        return (value == nullValue);
    }

    @Override
    public void         writeNull () {
        writeLong (nullValue);
    }

    @Override
    public void         writeString (CharSequence s) {
        writeLong (DataType.parseLong (s.toString ()));
    }

    @Override
    protected Object getBoxedValue() {
        return (isNull () ? null : value);
    }        
}