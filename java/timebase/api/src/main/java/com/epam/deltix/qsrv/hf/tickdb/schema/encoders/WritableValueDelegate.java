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
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

public class WritableValueDelegate implements MixedWritableValue {

    private final WritableValue writable;

    public WritableValueDelegate(WritableValue writable) {
        this.writable = writable;
    }

    public void writeBoolean(int value) {
        writable.writeBoolean(value != 0);
    }

    public void writeBoolean(long value) {
        writable.writeBoolean(value != 0);
    }

    public void writeBoolean(float value) {
        writable.writeBoolean(value != 0);
    }

    public void writeBoolean(double value) {
        writable.writeBoolean(value != 0);
    }

    public void writeEnum(CharSequence value) {
        writeString(value);
    }

    public void writeChar(char value) {
        writable.writeChar(value);
    }

    public void writeInt(long value) {
        writable.writeInt((int) value);
    }

    public void writeInt(double value) {
        writable.writeInt((int) value);
    }

    public void writeFloat(double value) {
        writable.writeFloat((float) value);
    }

    public void writeLong(float value) {
        writable.writeLong((long) value);
    }

    public void writeLong(double value) {
        writable.writeLong((long) value);
    }

    @Override
    public WritableValue nextWritableElement() {
        return writable.nextWritableElement();
    }

    public void writeBoolean(boolean value) {
        writable.writeBoolean(value);
    }

    public void writeInt(int value) {
        writable.writeInt(value);
    }

    public void writeLong(long value) {
        writable.writeLong(value);
    }

    public void writeFloat(float value) {
        writable.writeFloat(value);
    }

    public void writeDouble(double value) {
        writable.writeDouble(value);
    }

    public void writeString(CharSequence value) {
        writable.writeString(value);
    }

    @Override
    public void setArrayLength(int len) {
        writable.setArrayLength(len);
    }

    @Override
    public UnboundEncoder getFieldEncoder(RecordClassDescriptor rcd) {
        return writable.getFieldEncoder(rcd);
    }

    public void writeBinary(byte[] data, int offset, int length) {
        writable.writeBinary(data, offset, length);
    }

    public void                 writeNull() {
        writable.writeNull();
    }

    @Override
    public void                 writeDefault() {
        writable.writeNull();
    }

    @Override
    public MixedWritableValue   clone(WritableValue out) {
        return new WritableValueDelegate(out);
    }
}