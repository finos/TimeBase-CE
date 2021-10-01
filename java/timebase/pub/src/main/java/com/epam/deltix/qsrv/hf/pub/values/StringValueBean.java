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
 *  Holds a VARCHAR value.
 */
public final class StringValueBean extends ValueBean {
    private static final long serialVersionUID = 1L;

    private final StringBuilder value = new StringBuilder ();
    private boolean             isNull;
    private final boolean       isNullable;

    public StringValueBean (VarcharDataType type) {
        super (type);
        isNullable = type.isNullable ();
        isNull = isNullable;
    }

    public CharSequence getRaw () {
        return (isNull ? null : value);
    }

    @Override
    public String       getString () throws NullValueException {
        if (isNull)
            throw NullValueException.INSTANCE;

        return (value.toString ());
    }

    @Override
    public boolean      isNull () {
        return (isNull);
    }

    @Override
    public void         writeNull () {
        isNull = true;
    }

    @Override
    public void         writeString (CharSequence s) {
        value.setLength (0);
        value.append (s);
    }
    
    @Override
    protected Object getBoxedValue() {
        return (isNull ? null : value.toString ());
    } 
}