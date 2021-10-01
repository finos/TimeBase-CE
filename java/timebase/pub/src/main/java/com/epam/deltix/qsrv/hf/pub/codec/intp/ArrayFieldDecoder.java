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

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.IllegalNullValue;
import com.epam.deltix.qsrv.hf.pub.codec.validerrors.ValidationError;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.NoSuchElementException;

/*
 * Data format:
 * raw size - overall field size in bytes encoded via MessageSizeCodec. Zero size means NULL-value
 * array length - number of elements in array encoded via MessageSizeCodec.
 * array elements
 */
class ArrayFieldDecoder<T> extends FieldDecoder {
    final static int NULL_CODE = 0;
    private final ArraysAdapters.ArrayAdapter<T> arrayAdapter;

    // unbound API implementation
    private final MyReadableValue rv;
    private MemoryDataInput in;
    private int initialPosition;
    private int bodyLimit;
    private int firstElementPosition;
    private int arrayPosition;
    private int arrayLen;
    private int arrayIdx;
    private int commitedArrayIdx;
    private final int fixedSize;


    public ArrayFieldDecoder(TypeLoader loader, NonStaticFieldLayout f) {
        super(f);

        final DataType underlineDT = ((ArrayDataType) f.getType()).getElementDataType();
        final NonStaticFieldLayout underlineF = new NonStaticFieldLayout(f, new NonStaticDataField(f.getName()+"[]", null, underlineDT));
        final FieldDecoder fd = FieldCodecFactory.createDecoder(loader, underlineF);

        if (f.isBound()) {
            arrayAdapter = ArraysAdapters.createDecodeAdapter(fieldType, fd, underlineDT instanceof EnumDataType,
                    underlineDT instanceof VarcharDataType && ((VarcharDataType) underlineDT).getEncodingType() == VarcharDataType.INLINE_VARSIZE);
            rv = null;
            fixedSize = -1;
        } else {
            arrayAdapter = null;
            fixedSize = RecordLayout.getFixedSize(underlineDT);
            final DecodingContext ctxt = new DecodingContext(new RecordLayout(new RecordClassDescriptor(null, null, false, null)));
            rv = new MyReadableValue(ctxt, fd);
        }
    }

    @Override
    void            skip(DecodingContext ctxt) {
        skipField(ctxt);
    }

    private int     skipField(DecodingContext ctxt) {
        // field size
        int size = MessageSizeCodec.read(ctxt.in);
        int available = ctxt.in.getAvail();

        // we can have incomplete message
        int min = Math.min(size, available);
        ctxt.in.skipBytes(min);
        return min;
    }

    @Override
    protected void  copy(DecodingContext ctx, Object obj)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        // raw size
        final int size = MessageSizeCodec.read(ctx.in);
        if (size == NULL_CODE)
            setter.set(obj, null);
        else {
            final int limit = ctx.in.getPosition() + size;

            // array length
            final int len = MessageSizeCodec.read(ctx.in);
            Object array = arrayAdapter.initArray(len, ctx);

            for (int i = 0; i < len; i++) {
                if (ctx.in.getPosition() < limit)
                    arrayAdapter.decode(i, ctx);
                else
                    arrayAdapter.setNullElement(i);
            }

            // we always ignore an original object's field value on decoding
            setter.set(obj, array);
        }
    }

    @Override
    protected void setNull(Object obj) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        setter.set(obj, null);
    }

    @Override
    String getString(DecodingContext ctxt) {
        int i = skipField(ctxt);
        return i > 0 ? "ARRAY" : null;
    }

    @Override
    int getArrayLength() {
        // reset position
        if (initialPosition != in.getPosition())
            in.seek(initialPosition);

        // raw size
        final int size = MessageSizeCodec.read(in);
        if (size == NULL_CODE) {
            throw NullValueException.INSTANCE;
        } else {
            bodyLimit = size + in.getCurrentOffset();
            // array length
            arrayLen = MessageSizeCodec.read(in);
            arrayPosition = firstElementPosition = in.getPosition();
            return arrayLen;
        }
    }

    @Override
    ReadableValue nextReadableElement() {
        if (arrayLen == -1)
            throw new UnsupportedOperationException("getArrayLength was not called");
        if (++arrayIdx >= arrayLen)
            throw new NoSuchElementException("array boundary exeeded");

        rv.ctxt.in = in;

        if (firstElementPosition == -1)
            getArrayLength();

        if (fixedSize != -1) {
            final int pos = firstElementPosition + arrayIdx * fixedSize;
            if (pos + in.getStart() >= bodyLimit)
                throw NullValueException.INSTANCE;
            else
                in.seek(pos);
        } else if (commitedArrayIdx != -1) {
            // as an underline element might not be read, recalculate position of the next element
            in.seek(arrayPosition);
            rv.skip();

            if (in.getCurrentOffset() >= bodyLimit)
                throw NullValueException.INSTANCE;
        }

        arrayPosition = in.getPosition();
        return rv;
    }

    void reset(MemoryDataInput in) {
        this.in = in;
        initialPosition = this.in.getPosition();
        firstElementPosition = arrayPosition = arrayLen = commitedArrayIdx = arrayIdx = -1;
    }

    @Override
    int compare(DecodingContext ctxt1, DecodingContext ctxt2) {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public boolean isNull(DecodingContext ctxt) {
        return MessageSizeCodec.read(ctxt.in) == NULL_CODE;
    }

    private class MyReadableValue implements ReadableValue {
        private final DecodingContext ctxt;
        private final FieldDecoder fieldDecoder;

        private MyReadableValue(DecodingContext ctxt, FieldDecoder fieldDecoder) {
            this.ctxt = ctxt;
            this.fieldDecoder = fieldDecoder;
        }

        @Override
        public boolean isNull() {
            check();
            final boolean r = fieldDecoder.isNull(ctxt);
            commit();
            return r;
        }

        @Override
        public boolean getBoolean() throws NullValueException {
            check();
            final byte r = fieldDecoder.getByte(ctxt);
            handleNullable(fieldDecoder.isNull(r));
            commit();
            return r == BooleanDataType.TRUE;
        }

        @Override
        public char getChar() throws NullValueException {
            check();
            final char r = fieldDecoder.getChar(ctxt);
            commit();
            handleNullable(fieldDecoder.isNull(r));
            return r;
        }

        @Override
        public byte getByte() throws NullValueException {
            check();
            final byte b = fieldDecoder.getByte(ctxt);
            commit();
            handleNullable(fieldDecoder.isNull(b));
            return b;
        }

        @Override
        public short getShort() throws NullValueException {
            check();
            final short s = fieldDecoder.getShort(ctxt);
            commit();
            handleNullable(fieldDecoder.isNull(s));
            return s;
        }

        @Override
        public int getInt() throws NullValueException {
            check();
            final int r = fieldDecoder.getInt(ctxt);
            commit();
            handleNullable(fieldDecoder.isNull(r));
            return r;
        }

        @Override
        public long getLong() throws NullValueException {
            check();
            final long r = fieldDecoder.getLong(ctxt);
            commit();
            handleNullable(fieldDecoder.isNull(r));
            return r;
        }

        @Override
        public float getFloat() throws NullValueException {
            check();
            final float r = fieldDecoder.getFloat(ctxt);
            commit();
            handleNullable(fieldDecoder.isNull(r));
            return r;
        }

        @Override
        public double getDouble() throws NullValueException {
            check();
            final double r = fieldDecoder.getDouble(ctxt);
            commit();
            handleNullable(fieldDecoder.isNull(r));
            return r;
        }

        @Override
        public String getString() throws NullValueException {
            check();
            final String r = fieldDecoder.getString(ctxt);
            commit();
            handleNullable(fieldDecoder.isNull(r));
            return r;
        }

        @Override
        public int getArrayLength() throws NullValueException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ReadableValue nextReadableElement() throws NullValueException {
            throw new UnsupportedOperationException();
        }

        @Override
        public UnboundDecoder getFieldDecoder() throws NullValueException {
            check();
            try {
                // special init for ARRAY of OBJECT case
                if (fieldDecoder instanceof ClassFieldDecoder)
                    ((ClassFieldDecoder) fieldDecoder).reset(ctxt.in);

                final UnboundDecoder r = fieldDecoder.getFieldDecoder();
                commit();
                return r;
            } catch (NullValueException e) {
                commit();
                handleNullable(true);
                return null; // not reachable
            }
        }

        @Override
        public int getBinaryLength() throws NullValueException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getBinary(int offset, int length, OutputStream out) throws NullValueException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void getBinary(int srcOffset, int length, byte[] dest, int destOffset) throws NullValueException {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream openBinary() throws NullValueException {
            throw new UnsupportedOperationException();
        }

        // internally used function, so no check/commit
        private void skip() {
            fieldDecoder.skip(ctxt);
        }

        private void check() {
            if (commitedArrayIdx == arrayIdx)
                in.seek(arrayPosition);

            if (isOutOfData())
                handleNullable(true);
        }

        private void commit() {
            commitedArrayIdx = arrayIdx;
        }

        private boolean isOutOfData() {
            final MemoryDataInput in = ctxt.in;
            return in.getCurrentOffset() >= bodyLimit;
        }

        private void handleNullable(boolean isNull) {
            if (isNull) {
                if (fieldDecoder.isNullable)
                    throw NullValueException.INSTANCE;
                else
                    throw new IllegalStateException(fieldDecoder.getNotNullableMsg());
            }
        }
    }

    @Override
    public ValidationError validate (DecodingContext ctxt) {
        if (isNullable)
            skip (ctxt);
        else {
            in = ctxt.in;
            initialPosition = in.getPosition();
            arrayIdx = 0;

            final int offset = ctxt.in.getCurrentOffset ();
            final int len    = getArrayLength();    // read array SIZE and length (element count)

            if (arrayLen == NULL_CODE)
                return (new IllegalNullValue(offset, fieldInfo));

            for (int i = 0; i < len; i++) {
                rv.fieldDecoder.validate(ctxt);
            }
        }

        return (null);
    }
}