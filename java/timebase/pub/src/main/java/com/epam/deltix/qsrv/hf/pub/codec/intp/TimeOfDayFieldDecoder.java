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
import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class TimeOfDayFieldDecoder extends FixedSizeIntegerFieldDecoder {
    TimeOfDayFieldDecoder (NonStaticFieldLayout f) {
        super (f, 4);
    }

    @Override
    public String   getString (DecodingContext ctxt) {
        final int v = getInt (ctxt);
        assert isNullable || v != TimeOfDayDataType.NULL : getNotNullableMsg();
        return (v == TimeOfDayDataType.NULL ? null : TimeOfDayDataType.staticFormat (v));
    }

    @Override
    public boolean isNull(long value) {
        return value == TimeOfDayDataType.NULL;
    }

    @Override
    protected void setNull(Object obj) throws IllegalAccessException, InvocationTargetException {
        setter.setInt(obj, TimeOfDayDataType.NULL);
    }
}