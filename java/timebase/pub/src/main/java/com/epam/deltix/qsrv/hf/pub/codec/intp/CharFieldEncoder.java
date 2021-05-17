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

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class CharFieldEncoder extends FieldEncoder {
    CharFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final char v = getter.getChar(obj);
        if (!isNullable && v == CharDataType.NULL)
            throwNotNullableException();

        ctxt.out.writeChar(v);
    }

    void                    writeNull(EncodingContext ctxt) {
        ctxt.out.writeChar (CharDataType.NULL);
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        if (value == null || value.length() != 1)
            throw new IllegalArgumentException(String.valueOf(value));

        setChar(value.charAt(0), ctxt);
    }

    @Override
    void                    setChar (char value, EncodingContext ctxt) {
        if (!isNullable && value == CharDataType.NULL)
            throwNotNullableException();

        ctxt.out.writeChar (value);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return getter.getChar(message) == CharDataType.NULL;
    }
}
