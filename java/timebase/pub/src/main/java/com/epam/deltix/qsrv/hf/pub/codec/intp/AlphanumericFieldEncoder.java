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

import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.pub.codec.AlphanumericCodec;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;

import java.lang.reflect.InvocationTargetException;

/**
 * User: BazylevD
 * Date: Oct 21, 2009
 */
class AlphanumericFieldEncoder extends FieldEncoder {
    private final AlphanumericCodec codec;
    private final Types type;

    AlphanumericFieldEncoder (NonStaticFieldLayout f,  int length) {
        super (f);

        if (!f.isBound())
            type = Types.STRING;
        else {
            if (fieldType == long.class)
                type = Types.LONG;
            else if (fieldType == String.class || CharSequence.class.isAssignableFrom(fieldType))
                type = Types.STRING;
            else
                throw new IllegalArgumentException("Type " + fieldType + " is not supported.");
        }

        codec = new AlphanumericCodec(length);
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        if (type == Types.LONG)
            setLong(getter.getLong(obj), ctxt);
        else {
            final Object fieldValue = getter.get(obj);

            try {
                setString((CharSequence) fieldValue, ctxt);
            } catch (ClassCastException cx) {
                throw new ClassCastException(
                        fieldDescription + ": " + fieldValue.getClass().getName() + " cannot be cast to CharSequence"
                );
            }
        }
    }

    void                    writeNull(EncodingContext ctxt) {
        setString(null, ctxt);
    }

    @Override
    void setLong(long value, EncodingContext ctxt) {
        if (!isNullable && value == ExchangeCodec.NULL)
            throwNotNullableException();
        else
            codec.writeLong(value, ctxt.out);
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        if (!isNullable && value == null)
            throwNotNullableException();
        else
            codec.writeCharSequence(value, ctxt.out);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        if (type == Types.LONG)
            return getter.getLong(message) == ExchangeCodec.NULL;
        else
            return getter.get(message) == null;
    }

    @Override
    protected boolean isNull(long value) {
        return value == IntegerDataType.INT64_NULL;
    }

    private static enum Types {
        STRING,
        LONG
    }
}