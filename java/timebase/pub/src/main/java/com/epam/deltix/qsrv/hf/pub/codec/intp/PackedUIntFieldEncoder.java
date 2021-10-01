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
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.util.text.CharSequenceParser;

/**
 *
 */
class PackedUIntFieldEncoder extends IntegerFieldEncoder {
    public PackedUIntFieldEncoder (NonStaticFieldLayout f) {
        super (f);
    }

    @Override
    void                    writeNull(EncodingContext ctxt) {
        setLong(IntegerDataType.PUINT30_NULL, ctxt);
    }

    @Override
    protected boolean isNull(long value) {
        return value == IntegerDataType.PUINT30_NULL; 
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        setLong (CharSequenceParser.parseInt (value), ctxt);
    }

    @Override
    void                    setLongImpl (long value, EncodingContext ctxt) {
        CodecUtils.writePackedUnsignedInt((int) value, ctxt.out);
    }    
}