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
import com.epam.deltix.util.text.CharSequenceParser;

/**
 *
 */
class FixedSizeIntegerFieldEncoder extends IntegerFieldEncoder {
    private final int   size;
    private final int   index;

    FixedSizeIntegerFieldEncoder (NonStaticFieldLayout f, int size) {
        super (f);
        this.size = size;
        this.index = IntegerDataType.getIndex(size);
    }

    void                    writeNull(EncodingContext ctxt) {
        final long nullValue = IntegerDataType.NULLS[index];
        setLong(nullValue, ctxt);
    }

    @Override
    protected boolean isNull(long value) {
        return value == IntegerDataType.NULLS[index];
    }

    @Override
    void                    setString (CharSequence value, EncodingContext ctxt) {
        final long v;
        switch (size) {
            case 1:         v = CharSequenceParser.parseByte(value); break;
            case 2:         v = CharSequenceParser.parseShort(value); break;
            case 4:         v = CharSequenceParser.parseInt(value); break;
            case 6:
            case 8:         v = CharSequenceParser.parseLong(value); break;
            default:        throw new RuntimeException ("Illegal size: " + size);
        }
        setLong (v, ctxt);
    }

    @Override
    void                    setLongImpl (long value, EncodingContext ctxt) {
        switch (size) {
            case 1:         ctxt.out.writeByte ((byte) value);  break;
            case 2:         ctxt.out.writeShort ((short) value);  break;
            case 4:         ctxt.out.writeInt ((int) value);  break;
            case 6:         ctxt.out.writeLong48 (value);  break;
            case 8:         ctxt.out.writeLong (value);  break;
            default:        throw new RuntimeException ("Illegal size: " + size);
        }
    }
}