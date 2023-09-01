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

import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.codec.CodecUtils;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.text.CharSequenceParser;

import java.util.ArrayList;

/**
 *  Interpreting fixed unbound encoder.
 */
public final class FixedUnboundEncoderImpl implements FixedUnboundEncoder {
    private final RecordLayout      layout;
    private final EncodingContext   ctxt;
    private final FieldEncoder []   fields;
    private int                     fieldIdx;
    private FieldEncoder            currentField;
    private int                     currentPosition;

    // cached metadata
    private final boolean[]         isNullable;
    private final static int        STRING_IDX = 0;
    private final static int        INT_IDX = 1;
    private final static int        LONG_IDX = 2;
    private final static int        FLOAT_IDX = 3;
    private final static int        DOUBLE_IDX = 4;
    private final static Class<?>[] TYPE_CLASSES = {String.class, int.class, long.class, float.class, double.class};
    private final int[][]           limitRef;
    private final Number[]          min;
    private final Number[]          max;
    
    public FixedUnboundEncoderImpl (RecordLayout layout) {
        this.layout = layout;

        fields = FieldCodecFactory.createEncoders (layout);
        ctxt = new EncodingContext (layout);

        // populate metadata cache
        final NonStaticFieldLayout[] nonStaticFields = layout.getNonStaticFields();
        if (nonStaticFields != null) {
            isNullable = new boolean[nonStaticFields.length];
            limitRef = new int[DOUBLE_IDX + 1][nonStaticFields.length];
            ArrayList<Number> minLimits = new ArrayList<Number>();
            ArrayList<Number> maxLimits = new ArrayList<Number>();
            for (int i = 0; i < nonStaticFields.length; i++) {
                final DataType dataType = nonStaticFields[i].getField().getType();
                isNullable[i] = dataType.isNullable();
                for (int j = STRING_IDX; j <= DOUBLE_IDX; j++) {
                    if ((dataType instanceof IntegerDataType || dataType instanceof FloatDataType) &&
                            (j == STRING_IDX ||
                                    (dataType instanceof IntegerDataType && j < FLOAT_IDX) ||
                                    (dataType instanceof FloatDataType && j >= FLOAT_IDX))) {
                        final Number min = CodecUtils.getMinLimit(dataType, false, TYPE_CLASSES[j]);
                        final Number max = CodecUtils.getMaxLimit(dataType, false, TYPE_CLASSES[j]);
                        if (min != null || max != null) {
                            limitRef[j][i] = minLimits.size();
                            minLimits.add(min);
                            maxLimits.add(max);
                        } else
                            limitRef[j][i] = -1;
                    } else
                        limitRef[j][i] = -1;
                }
            }
            if (minLimits.size() > 0) {
                min = minLimits.toArray(new Number[minLimits.size()]);
                max = maxLimits.toArray(new Number[minLimits.size()]);
            } else {
                min = null;
                max = null;
            }
        } else {
            isNullable = null;
            limitRef = null;
            min = null;
            max = null;
        }
    }

    public RecordClassInfo      getClassInfo () {
        return (layout);
    }
        
    public void                 beginWrite (MemoryDataOutput out) {
        ctxt.out = out;
        fieldIdx = -1;
        currentField = null;
        currentPosition = -1;
    }

    @Override
    public void                 endWrite() {

        // special check for ARRAY and OBJECT fields
        if (currentField instanceof ContainerEncoder)
            ((ContainerEncoder)currentField).endWrite();

        if (fieldIdx >= fields.length - 1)
            return;

        for (int i = fieldIdx + 1; i < fields.length; i++) {
            if (!fields[i].isNullable)
                throw new IllegalArgumentException(String.format("%s field is not nullable", fields[i].fieldName));
        }
    }

    public NonStaticFieldLayout getField () {
        return (layout.getNonStaticFields () [fieldIdx]);
    }

    public boolean              nextField () {

        // special check for ARRAY and OBJECT fields
        if (currentField instanceof ContainerEncoder)
            ((ContainerEncoder)currentField).endWrite();

        if (currentPosition == ctxt.out.getPosition() && fieldIdx < fields.length - 1)
            writeNull();

        fieldIdx++;

        if (fieldIdx >= fields.length) {
            currentField = null;
            return (false);
        }

        currentField = fields[fieldIdx];
        currentPosition = ctxt.out.getPosition();

        // special check for ARRAY and OBJECT fields
        if (currentField instanceof ContainerEncoder)
            ((ContainerEncoder)currentField).beginWrite(ctxt.out);

        return (true);
    }

    public void writeNull() {
        resetPosition();
        if (!isNullable[fieldIdx])
            throw new IllegalArgumentException("'" + getField().getName() + "' field is not nullable");
        currentField.writeNull(ctxt);
    }

    public void                 writeBoolean (boolean value) {
        resetPosition();
        currentField.setBoolean (value, ctxt);
    }

    public void writeChar(char value) {
        resetPosition();
        currentField.setChar (value, ctxt);
    }

    public void                 writeInt (int value) {
        resetPosition();
        if (currentField.isNull(value))
            writeNull();
        else {
            checkLimit(INT_IDX, value);
            currentField.setInt(value, ctxt);
        }
    }

    public void                 writeLong (long value) {
        resetPosition();
        if (currentField.isNull(value))
            writeNull();
        else {
            checkLimit(LONG_IDX, value);
            currentField.setLong(value, ctxt);
        }
    }

    public void                 writeFloat (float value) {
        resetPosition();
        if (Float.isNaN(value))
            writeNull();
        else {
            checkLimit(FLOAT_IDX, value);
            currentField.setFloat(value, ctxt);
        }
    }

    public void                 writeDouble (double value) {
        resetPosition();
        if (Double.isNaN(value))
            writeNull();
        else {
            checkLimit(DOUBLE_IDX, value);
            currentField.setDouble(value, ctxt);
        }
    }

    public void                 writeString (CharSequence value) {
        resetPosition();
        if (currentField.isNull(value))
            writeNull();
        else {
            checkLimit(STRING_IDX, value);
            currentField.setString(value, ctxt);
        }
    }

    @Override
    public void setArrayLength(int len) {
        resetPosition();
        currentField.setArrayLength(len, ctxt);
    }

    @Override
    public WritableValue nextWritableElement() {
        // TODO: check that setArrayLength was called
        return currentField.nextWritableElement();
    }

    @Override
    public UnboundEncoder getFieldEncoder(RecordClassDescriptor rcd) {
        return currentField.getFieldEncoder(rcd);
    }

    @Override
    public void                 writeBinary(byte[] data, int offset, int length) {
        resetPosition();
        if (data == null)
            writeNull();
        else
            currentField.setBinary(data, offset, length, ctxt);
    }

    private void resetPosition() {
        if (currentPosition != ctxt.out.getPosition())
            ctxt.out.seek(currentPosition);
    }

    private void checkLimit(int type, long value) {
        final int idx = limitRef[type][fieldIdx];
        if (idx != -1) {
            if (min[idx] != null && min[idx].longValue() > value)
                throw new IllegalArgumentException(String.valueOf(value));
            if (max[idx] != null && max[idx].longValue() < value)
                throw new IllegalArgumentException(String.valueOf(value));
        }
    }

    private void checkLimit(int type, double value) {
        final int idx = limitRef[type][fieldIdx];
        if (idx != -1) {
            if (min[idx] != null && min[idx].doubleValue() > value)
                throw new IllegalArgumentException(String.valueOf(value));
            if (max[idx] != null && max[idx].doubleValue() < value)
                throw new IllegalArgumentException(String.valueOf(value));
        }
    }

    private void checkLimit(int type, CharSequence value) {
        final int idx = limitRef[type][fieldIdx];
        if (idx != -1) {
            final Number limit = min[idx] != null ? min[idx] : max[idx];
            final boolean isReal = limit instanceof Double || limit instanceof Float;
            final double doubleValue = isReal ? CharSequenceParser.parseDouble(value) : Double.NaN;
            final long longValue = !isReal ? CharSequenceParser.parseLong(value) : 0;

            if (min[idx] != null &&
                    ((isReal && min[idx].doubleValue() > doubleValue) ||
                            (!isReal && min[idx].longValue() > longValue)))
                throw new IllegalArgumentException(String.valueOf(value));

            if (max[idx] != null &&
                    ((isReal && max[idx].doubleValue() < doubleValue) ||
                            (!isReal && max[idx].longValue() < longValue)))
                throw new IllegalArgumentException(String.valueOf(value));
        }
    }
}