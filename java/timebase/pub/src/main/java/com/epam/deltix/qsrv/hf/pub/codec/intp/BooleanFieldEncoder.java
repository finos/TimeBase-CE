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

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.util.text.CharSequenceParser;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class BooleanFieldEncoder extends FieldEncoder {
    BooleanFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        byte            b;

        if (getter instanceof JavaValueGetterMethod) {
            if (fieldType == boolean.class) {
                if (!isNullable) {
                    b = getter.getBoolean(obj) ? BooleanDataType.TRUE : BooleanDataType.FALSE;
                } else {
                    JavaValueGetterMethod getterMethod = (JavaValueGetterMethod) getter;
                    if (getterMethod.hasHaser()) {
                        if (getterMethod.hasValue(obj))
                            b = getter.getBoolean(obj) ? BooleanDataType.TRUE : BooleanDataType.FALSE;
                        else
                            b = BooleanDataType.NULL;
                    } else
                        throw new UnsupportedOperationException("Can`t read NULLABLE BOOLEAN using getter: \"" + getterMethod.getGetterName() + "()\" that return boolean.class without HASER method.");
                }
            }  else if (fieldType == byte.class) {
                b = getter.getByte(obj);
                if (! isNullable && b == BooleanDataType.NULL)
                    throwNotNullableException();
            } else
                throw new RuntimeException("Unsupported field type: " + fieldType);

        } else {
            if (fieldType == boolean.class) {
                b = getter.getBoolean(obj) ? BooleanDataType.TRUE : BooleanDataType.FALSE;
            } else if (fieldType == byte.class) {
                b = getter.getByte(obj);
                if (! isNullable && b == BooleanDataType.NULL)
                    throwNotNullableException();
            } else
                throw new RuntimeException("Unsupported field type: " + fieldType);
        }
        ctxt.out.writeByte (b);
    }

    void                    writeNull(EncodingContext ctxt) {
        ctxt.out.writeByte (BooleanDataType.NULL);
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        setBoolean (CharSequenceParser.parseBoolean (value), ctxt);
    }

    @Override
    void                    setBoolean (boolean value, EncodingContext ctxt) {
        ctxt.out.writeBoolean (value);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        if (getter instanceof JavaValueGetterMethod && fieldType == boolean.class) {
            final JavaValueGetterMethod getterMethod = (JavaValueGetterMethod) getter;
            return getterMethod.hasHaser() && !getterMethod.hasValue(message);
        } else
            return fieldType == byte.class && getter.getByte(message) == BooleanDataType.NULL;
    }

    protected boolean isNull(long value) {
        // used when bound with byte field
        return value == -1;
    }
}