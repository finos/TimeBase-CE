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
package com.epam.deltix.qsrv.hf.pub.values;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.lang.Util;

/**
 *  Holds a BOOLEAN value.
 */
public final class BooleanValueBean extends ValueBean {
    private static final long serialVersionUID = 1L;

    private byte                 value;
    private final boolean       isNullable;

    public BooleanValueBean (VarcharDataType type) {
        super (type);
        isNullable = type.isNullable ();
        value = isNullable ? BooleanDataType.NULL : BooleanDataType.FALSE;
    }

    public int          getRaw () {
        return (value);
    }

    @Override
    public boolean      getBoolean () throws NullValueException {
        if (value == BooleanDataType.NULL)
            throw NullValueException.INSTANCE;

        return (value == BooleanDataType.TRUE);
    }

    @Override
    public byte getByte() throws NullValueException {
        return value;
    }

    @Override
    public String       getString () throws NullValueException {
        return (String.valueOf (getBoolean ()));
    }

    @Override
    public boolean      isNull () {
        return (value == BooleanDataType.NULL);
    }

    @Override
    public void         writeNull () {
        value = BooleanDataType.NULL;
    }

    @Override
    public void         writeBoolean (boolean value) {
        this.value = value ? BooleanDataType.TRUE : BooleanDataType.FALSE;
    }

    @Override
    public void         writeString (CharSequence value) {
        writeBoolean (Util.equals (value, "true"));
    }
    
    @Override
    protected Object getBoxedValue() {
        return (isNull () ? null : getBoolean());
    }
}