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
package com.epam.deltix.test.qsrv.hf.tickdb.schema;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.time.GMT;

import java.text.ParseException;

/**
 *
 */
public abstract class UnboundUtils {

    static String readObject(ReadableValue udec) throws NullValueException {
        try {
            final UnboundDecoder decoder = udec.getFieldDecoder();
            final StringBuilder sb = new StringBuilder();
            sb.append(decoder.getClassInfo().getDescriptor().getName()).append(":[");
            // dump field/value pairs
            while (decoder.nextField()) {
                sb.append(decoder.getField().getName()).append("=");
                try {
                    sb.append(decoder.getString()).append(',');
                } catch (NullValueException e) {
                    sb.append("null,");
                }
            }
            sb.append(']');
            return sb.toString();
        } catch (NullValueException e) {
            return "null";
        }
    }

    static String readArray(ArrayDataType type, ReadableValue udec) throws NullValueException {
        final StringBuilder sb = new StringBuilder();
        final int len = udec.getArrayLength();
        final DataType underlineType = type.getElementDataType();

        sb.append('[');
        for (int i = 0; i < len; i++) {
            try {
                final ReadableValue rv = udec.nextReadableElement();

                if (underlineType instanceof FloatDataType)
                    sb.append(rv.getDouble());
                else if (underlineType instanceof IntegerDataType)
                    sb.append(rv.getLong());
                else if (underlineType instanceof ClassDataType)
                    sb.append(readObject(rv));
                else
                    throw new IllegalArgumentException(underlineType + " is not expected");

                sb.append(',');
            } catch (NullValueException e) {
                sb.append("null,");
            }
        }

        sb.append(']');
        return sb.toString();
    }

    static void writeObjectPoly(Object[] values, WritableValue uenc) {
        final int len = values.length;
        final UnboundEncoder encoder = uenc.getFieldEncoder((RecordClassDescriptor)values[0]);

        for (int i = 1; i < len && encoder.nextField(); i++) {
            Object v = values[i];
            if (v == null)
                continue;

            if (v instanceof Double)
                encoder.writeDouble((Double) v);
            else if (v instanceof String) {
                if (encoder.getField().getType() instanceof DateTimeDataType)
                    try {
                        encoder.writeLong(GMT.parseDate((String) v).getTime());
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                else
                    encoder.writeString((CharSequence) v);
            } else
                throw new IllegalArgumentException("unsupported type " + v.getClass().getName());
        }
    }
}
