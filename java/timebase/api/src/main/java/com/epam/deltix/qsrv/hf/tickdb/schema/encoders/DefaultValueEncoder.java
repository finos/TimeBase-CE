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
package com.epam.deltix.qsrv.hf.tickdb.schema.encoders;

import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;

import java.util.HashSet;


public class DefaultValueEncoder implements MixedWritableValue {
    protected final WritableValue writable;

    protected final String        defaultValue;
    protected final DataType      type;

    private Number          minValue;
    private Number          maxValue;
    private HashSet<String> enumValues;

    protected DefaultValueEncoder(WritableValue encoder, DataType type) {
        this(encoder, null, type);
    }

    public DefaultValueEncoder(WritableValue encoder, String defaultValue, DataType type) {

        //TODO: convert default value to target type to boost performance
        this.writable = encoder;
        this.defaultValue = defaultValue;
        this.type = type;

        if (type instanceof FloatDataType) {
            Number[] range = ((FloatDataType) type).getRange();
            minValue = range[0];
            maxValue = range[1];
        }
        else if (type instanceof IntegerDataType) {
            Number[] range = ((IntegerDataType) type).getRange();
            minValue = range[0];
            maxValue = range[1];
        }
        else if (type instanceof DateTimeDataType) {
            minValue = Long.MIN_VALUE;
            maxValue = Long.MAX_VALUE;
        }
        else if (type instanceof TimeOfDayDataType) {
            minValue = 0;
            maxValue = Integer.MAX_VALUE;
        }
        else if (type instanceof EnumDataType) {
            enumValues = new HashSet<String>();
            EnumValue[] values = ((EnumDataType) type).descriptor.getValues();
            for (EnumValue value : values)
                enumValues.add(value.symbol);

            minValue = 0;
            maxValue = Long.MAX_VALUE;
        }
    }

    public void                 writeDefault() {
        if (defaultValue == null)
            writeNull();
        else
            writable.writeString(defaultValue);
    }

    public void                 writeNull () {
        writable.writeNull();
    }

    public void writeBoolean(int value) {
        writeDefault();
    }

    public void writeBoolean(long value) {
        writeDefault();
    }

    public void writeBoolean(float value) {
        writeDefault();
    }

    public void writeBoolean(double value) {
        writeDefault();
    }

    public void writeBoolean(boolean value) {
        writable.writeBoolean(value);
    }

    public void writeChar(char value) {
        writable.writeChar(value);
    }

    public void                 writeInt (int value) {
        if (value < minValue.intValue())
            writeDefault();
        else if (value > maxValue.intValue())
            writeDefault();
        else
            writable.writeInt(value);
    }

    public void                 writeInt (double value) {
        if (value < minValue.intValue())
            writeDefault();
        else if (value > maxValue.intValue())
            writeDefault();
        else
            writable.writeInt((int)value);
    }

    public void writeInt(long value) {
         if (value < minValue.intValue())
            writeDefault();
        else if (value > maxValue.intValue())
            writeDefault();
        else
            writable.writeInt((int)value);
    }

    public void writeFloat(double value) {
          if (value < minValue.floatValue())
            writeDefault();
        else if (value > maxValue.floatValue())
            writeDefault();
        else
            writable.writeFloat((float)value);
    }

    public void writeLong(float value) {
        if (value < minValue.longValue())
            writeDefault();
        else if (value > maxValue.longValue())
            writeDefault();
        else
            writable.writeLong((long)value);
    }

    public void writeLong(double value) {
         if (value < minValue.longValue())
            writeDefault();
        else if (value > maxValue.longValue())
            writeDefault();
        else
            writable.writeLong((long)value);
    }

    public void                 writeLong (long value) {
        if (value < minValue.longValue())
            writeDefault();
        else if (value > maxValue.longValue())
            writeDefault();
        else
            writable.writeLong(value);
    }

    public void                 writeFloat (float value) {
        if (value < minValue.floatValue())
            writeDefault();
        else if (value > maxValue.floatValue())
            writeDefault();
        else
            writable.writeFloat(value);
    }

    public void                 writeDouble (double value) {
        if (value < minValue.doubleValue())
            writeDefault();
        else if (value > maxValue.doubleValue())
            writeDefault();
        else
            writable.writeDouble(value);
    }

    public void                 writeString (CharSequence value) {
        try {
            writable.writeString(value);
        } catch (Exception e) {
            //TODO: log error
            writeDefault();
        }
    }

    @Override
    public void setArrayLength(int len) {
        writable.setArrayLength(len);
    }

    @Override
    public WritableValue nextWritableElement() {
        return writable.nextWritableElement();
    }

    @Override
    public UnboundEncoder getFieldEncoder(RecordClassDescriptor rcd) {
        return writable.getFieldEncoder(rcd);
    }

    public void                 writeBinary(byte[] data, int offset, int length) {
        writable.writeBinary(data, offset, length);
    }
    
    public void writeEnum(CharSequence value) {
        if (enumValues != null && enumValues.contains(value.toString()))
            writable.writeString(value);
        else
            writeDefault();
    }

    @Override
    public MixedWritableValue clone(WritableValue out) {
        return new DefaultValueEncoder(out, type);
    }
}