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

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.WritableValue;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Supports binding to .NET properties
 */
public abstract class PropertySupport implements WritableValue, ReadableValue {
    public Object msg;

    // WritableValue

    @Override
    public void writeString(CharSequence value) {
        throwException();
    }

    @Override
    public void setArrayLength(int len) {
        throwException();
    }

    @Override
    public WritableValue nextWritableElement() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeBinary(byte[] data, int offset, int length) {
        throwException();
    }

    @Override
    public void writeNull() {
        throwException();
    }

    // ReadableValue

    @Override
    public InputStream openBinary() throws NullValueException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isNull() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getString() throws NullValueException {
        throw new UnsupportedOperationException();
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

    public abstract void writeObject(Object value);
    public abstract Object readObject();

    protected static void throwException() {
        throw new UnsupportedOperationException();
    }
}