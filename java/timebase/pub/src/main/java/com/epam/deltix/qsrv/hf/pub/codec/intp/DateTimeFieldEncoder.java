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
import com.epam.deltix.qsrv.hf.pub.md.DateTimeDataType;
import com.epam.deltix.util.time.GMT;

import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;

/**
 *
 */
class DateTimeFieldEncoder extends FieldEncoder {
    DateTimeFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    final protected void copy (Object obj, EncodingContext ctxt)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        final long t = getter.getLong(obj);
        setLong (t, ctxt);
    }

    void                    writeNull(EncodingContext ctxt) {
        setLong(DateTimeDataType.NULL, ctxt);
    }

    @Override
    protected boolean isNull(long value) {
        return value == DateTimeDataType.NULL; 
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        long        t;
        
        try {
            t = GMT.parseDateTimeMillis (value.toString()).getTime ();
            setLong (t, ctxt);
        } catch (ParseException x) {
            throwConstraintViolationException(value);
        }
    }

    @Override
    void                    setLong (long value, EncodingContext ctxt) {
        if (!isNullable && value == DateTimeDataType.NULL)
            throwNotNullableException();
        else
            ctxt.out.writeLong(value);
    }

    @Override
    protected boolean isNullValue(Object message) throws IllegalAccessException, InvocationTargetException {
        final long value = getter.getLong(message);
        return isNull(value);
    }
}
