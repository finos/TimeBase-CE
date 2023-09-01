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

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.ValidationError;
import com.epam.deltix.util.lang.MathUtil;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 *  Interpreting fixed unbound record decoder.
 */
public class FixedUnboundDecoderImpl implements UnboundDecoder {
    final RecordLayout              layout;
    final DecodingContext           ctxt;
    DecodingContext                 ctxt2 = null;
    final FieldDecoder []           fields;
    private int                     fieldIdx;
    private FieldDecoder            currentField;
    private int                     currentPosition;
    private int[]                   positions;
    // used for embedded OBJECT field case
    int                             bodyLimit = -1;
    private final boolean           hasPrimaryKey;

    public FixedUnboundDecoderImpl (RecordLayout layout) {
        this.layout = layout;
        this.fields = FieldCodecFactory.createDecoders (layout);
        this.ctxt = new DecodingContext (layout);
        this.positions = new int[this.fields.length];

        // check that we have Primary Key defined
        NonStaticFieldLayout[] fields = layout.getNonStaticFields();
        hasPrimaryKey = fields != null && Arrays.stream(fields).anyMatch(NonStaticFieldLayout::isPrimaryKey);
    }

    public RecordClassInfo      getClassInfo () {
        return (layout);
    }

    public void                 beginRead (MemoryDataInput in) {
        ctxt.in = in;
        fieldIdx = -1;
        currentField = null;
        currentPosition = -1;        
    }

    public NonStaticFieldInfo   getField () {
        return (layout.getNonStaticFields () [fieldIdx]);
    }

    public boolean              nextField () {
        fieldIdx++;
        if (currentPosition == ctxt.in.getPosition() && fieldIdx < fields.length && !isOutOfData())
            currentField.skip(ctxt);

        if (fieldIdx >= fields.length) {
            currentField = null;
            return (false);
        }

        currentField = fields [fieldIdx];
        currentPosition = ctxt.in.getPosition();
        positions[fieldIdx] = currentPosition;

        // special init for ARRAY and OBJECT fields
        if (currentField instanceof ArrayFieldDecoder)
            ((ArrayFieldDecoder) currentField).reset(ctxt.in);
        else if (currentField instanceof ClassFieldDecoder)
            ((ClassFieldDecoder) currentField).reset(ctxt.in);

        return (true);
    }

    public boolean              previousField() {

        if (fieldIdx >= 0)
            fieldIdx--;
        
        if (fieldIdx < 0) {
            currentField = null;
            return (false);
        }

        currentField = fields [fieldIdx];
        currentPosition = positions[fieldIdx];
        // special init for ARRAY and OBJECT fields
        if (currentField instanceof ArrayFieldDecoder) {
            resetPosition();
            ((ArrayFieldDecoder) currentField).reset(ctxt.in);
        } else if (currentField instanceof ClassFieldDecoder) {
            resetPosition();
            ((ClassFieldDecoder) currentField).reset(ctxt.in);
        }

        return (true);
    }

    public boolean              seekField (int index) {
        if (index >=0 && index < fields.length) {

            while (fieldIdx < index)
                nextField();
            while (index < fieldIdx)
                previousField();
        }

        return fieldIdx == index;
    }

    public boolean isNull() {
        resetPosition();
        // always returns FALSE for non-nullable field  
        return (currentField.isNullable && (isOutOfData() || currentField.isNull(ctxt)));
    }

    public boolean              getBoolean () {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        final byte v = currentField.getByte(ctxt);
        handleNullable(currentField.isNull(v));
        return (v != 0);
    }

    public char getChar() throws NullValueException {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        final char v = currentField.getChar(ctxt);
        handleNullable(currentField.isNull(v));
        return v;
    }

    @Override
    public byte getByte() throws NullValueException {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        final byte v = currentField.getByte(ctxt);
        handleNullable(currentField.isNull(v));
        return v;
    }

    @Override
    public short getShort() throws NullValueException {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        final short v = currentField.getShort(ctxt);
        handleNullable(currentField.isNull(v));
        return v;
    }

    @Override
    public int                  getInt () {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        final int v = currentField.getInt (ctxt);
        handleNullable(currentField.isNull(v));
        return v;
    }

    @Override
    public long                 getLong () {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        final long v = currentField.getLong (ctxt);
        handleNullable(currentField.isNull(v));
        return v;
    }

    @Override
    public float                getFloat () {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        final float v = currentField.getFloat (ctxt);
        handleNullable(currentField.isNull(v));
        return v;
    }

    @Override
    public double               getDouble () {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        final double v = currentField.getDouble (ctxt);
        handleNullable(currentField.isNull(v));
        return v;
    }

    @Override
    public String               getString () {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        final String v = currentField.getString (ctxt);
        handleNullable(currentField.isNull(v));
        return v;
    }

    @Override
    public int getArrayLength() throws NullValueException {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);

        return currentField.getArrayLength();
    }

    @Override
    public ReadableValue nextReadableElement() throws NullValueException {
        return currentField.nextReadableElement();
    }

    @Override
    public UnboundDecoder getFieldDecoder() throws NullValueException {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);

        return currentField.getFieldDecoder();
    }

    protected void              setUpForCompare (
        MemoryDataInput             in1,
        MemoryDataInput             in2
    ) {
        if (ctxt2 == null)
            ctxt2 = new DecodingContext (layout);

        ctxt.in = in1;
        ctxt2.in = in2;
    }

    @Override
    public int                  compareAll (
        MemoryDataInput             in1, 
        MemoryDataInput             in2
    ) {
        setUpForCompare (in1, in2);

        for (FieldDecoder df : fields) {
            final boolean isOutOfData1 = isOutOfData();
            final boolean isOutOfData2 = isOutOfData(ctxt2.in);
            if (isOutOfData1 && isOutOfData2)
                return 0;
            if (isOutOfData1)
                return df.isNull(ctxt2) ? 0 : -1;
            else if (isOutOfData2)
                return df.isNull(ctxt) ? 0 : 1;

            int     dif = df.compare (ctxt, ctxt2);
            
            if (dif != 0)
                return (dif);
        }
        
        return (0); 
    }

    @Override
    public int                  comparePrimaryKeys (
            MemoryDataInput             in1,
            MemoryDataInput             in2
    ) {
        if (!hasPrimaryKey)
            return -2;

        setUpForCompare (in1, in2);

        final int                       numFields = fields.length;
        final NonStaticFieldLayout []   fieldLayouts = layout.getNonStaticFields ();

        for (int ii = 0; ii < numFields; ii++) {
            final FieldDecoder          fd = fields [ii];

            final boolean isOutOfData1 = isOutOfData(ctxt.in);
            final boolean isOutOfData2 = isOutOfData(ctxt2.in);
            if (isOutOfData1 && isOutOfData2)
                return 0;

            if (fieldLayouts [ii].getField ().isPk()) {

                if (isOutOfData1)
                    return fd.isNull(ctxt2) ? 0 : -1;
                else if (isOutOfData2)
                    return fd.isNull(ctxt) ? 0 : 1;

                int result = fd.compare (ctxt, ctxt2);
                if (result != 0)
                    return MathUtil.sign(result);
            } else {
                if (!isOutOfData1)
                    fd.skip(ctxt);
                if (!isOutOfData2)
                    fd.skip(ctxt2);
            }
        }

        return 0;
    }

    @Override
    public int                  getBinaryLength() throws NullValueException {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        return currentField.getBinaryLength(ctxt);
    }

    @Override
    public void                 getBinary(int offset, int length, OutputStream out) throws NullValueException {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        currentField.getBinary(offset, length, out, ctxt);
    }

    @Override
    public void                 getBinary(int srcOffset, int length, byte [] dest, int destOffset) throws NullValueException {
        resetPosition();
        if (isOutOfData())
            handleNullable(true);
        currentField.getBinary(dest, srcOffset, destOffset, length, ctxt);
    }

    @Override
    public InputStream openBinary() {
        throw new UnsupportedOperationException();
    }

    private void resetPosition() {
        ctxt.in.seek(currentPosition);
    }

    private void handleNullable(boolean isNull) {
        if (isNull) {
            final NonStaticFieldInfo field = getField();
            if (field.getType().isNullable())
                throw NullValueException.INSTANCE;
            else
                throw new IllegalStateException("'" + field.getName() + "' field is not nullable");
        }
    }

    private boolean isOutOfData() {
        final MemoryDataInput in = ctxt.in;
        return bodyLimit != -1 ?
                in.getCurrentOffset() >= bodyLimit :
                in.getPosition() == in.getLength();
    }

    private static boolean isOutOfData(MemoryDataInput in) {
        return in.getPosition() == in.getLength();
    }

    public ValidationError validate () {
        final MemoryDataInput in = ctxt.in;
        boolean isTruncated = false;
        for (FieldDecoder decoder : fields) {
            if (!(isTruncated||(isTruncated = in.getAvail ()<=0))) {
                ValidationError err = decoder.validate(ctxt);
                if (err != null)
                    return err;
            }
        }

        // contract: we have limit for reading data and we should skip all unread content
        int available = in.getAvail();
        if (available > 0)
            in.skipBytes(available);

        return null;
    }

}