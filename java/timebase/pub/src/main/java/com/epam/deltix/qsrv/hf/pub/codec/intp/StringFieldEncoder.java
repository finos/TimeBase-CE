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

import com.epam.deltix.qsrv.hf.codec.BinaryUtils;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.util.collections.generated.ByteArrayList;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class StringFieldEncoder extends FieldEncoder {
    StringFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final Object fieldValue = getter.get(obj);
        if (!isNullable && fieldValue == null)
            throwNotNullableException();

        CharSequence      s;
        
        try {
            if (fieldValue instanceof ByteArrayList)
                s = BinaryUtils.toStringBuilder((ByteArrayList) fieldValue);
            else
                s = (CharSequence) fieldValue;
        } catch (ClassCastException cx) {
            throw new ClassCastException (
                fieldDescription + ": " + fieldValue.getClass().getName() + " cannot be cast to CharSequence"
            );
        }

        setString (s, ctxt);
    }

    void                    writeNull(EncodingContext ctxt) {
        setString(null, ctxt);
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        ctxt.out.writeString (value);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        return getter.get(message) == null;
    }
}