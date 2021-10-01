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
package com.epam.deltix.qsrv.hf.pub.values;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * Default implementation for value beans.
 */
public abstract class ValueBean
        implements ReadableValue, WritableValue, Serializable {
    private static final long serialVersionUID = 1L;

    public static ValueBean forType(DataType type) {
        Class<?> tc = type.getClass();

        if (tc == IntegerDataType.class)
            return (new IntegerValueBean((IntegerDataType) type));

        if (tc == FloatDataType.class) {
            FloatDataType fdt = (FloatDataType) type;

            return (fdt.isFloat() ? new FloatValueBean(fdt) : new DoubleValueBean(fdt));
        }

        if (tc == VarcharDataType.class)
            return (new StringValueBean((VarcharDataType) type));

        throw new UnsupportedOperationException("Not yet implemented for " + tc.getSimpleName());
    }

    public final DataType type;

    protected ValueBean(DataType type) {
        this.type = type;
    }

    public void checkNotNull() {
        if (isNull())
            throw NullValueException.INSTANCE;
    }

    @Override
    public void getBinary(int offset, int length, OutputStream out) throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public void getBinary(int srcOffset, int length, byte[] dest, int destOffset) throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public int getBinaryLength() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public boolean getBoolean() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public char getChar() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public double getDouble() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public float getFloat() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public byte getByte() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public short getShort() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public int getInt() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public long getLong() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public int getArrayLength() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public ReadableValue nextReadableElement() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public UnboundDecoder getFieldDecoder() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public InputStream openBinary() throws NullValueException {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public void writeBinary(byte[] data, int offset, int length) {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public void writeBoolean(boolean value) {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public void writeChar(char value) {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public void writeDouble(double value) {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public void writeFloat(float value) {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public void writeInt(int value) {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public void writeLong(long value) {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public void setArrayLength(int len) {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public WritableValue nextWritableElement() {
        throw new UnsupportedOperationException("Illegal");
    }

    @Override
    public UnboundEncoder getFieldEncoder(RecordClassDescriptor rcd) {
        throw new UnsupportedOperationException("Illegal");
    }

    protected abstract Object getBoxedValue();

    @Override
    public String toString() {
        return (type.toString(getBoxedValue()));
    }
}