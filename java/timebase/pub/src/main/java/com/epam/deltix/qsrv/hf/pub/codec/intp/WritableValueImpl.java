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


import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataOutput;

public abstract class WritableValueImpl implements WritableValue {

    FieldEncoder                encoder;
    private EncodingContext     ctx;

    protected WritableValueImpl(FieldEncoder encoder, EncodingContext ctx) {
        this.encoder = encoder;
        this.ctx = ctx;
    }

    @Override
    public void writeBoolean(boolean value) {
        encoder.setBoolean(value, ctx);
    }

    @Override
    public void writeChar(char value) {
        encoder.setChar(value, ctx);
    }

    @Override
    public void writeInt(int value) {
        encoder.setInt(value, ctx);
    }

    @Override
    public void writeLong(long value) {
        encoder.setLong(value, ctx);
    }

    @Override
    public void writeFloat(float value) {
        encoder.setFloat(value, ctx);
    }

    @Override
    public void writeDouble(double value) {
        encoder.setDouble(value, ctx);
    }

    @Override
    public void writeString(CharSequence value) {
        encoder.setString(value, ctx);
    }

    @Override
    public void setArrayLength(int len) {
        encoder.setArrayLength(len, ctx);
    }

    @Override
    public WritableValue nextWritableElement() {
        return encoder.nextWritableElement();
    }

    @Override
    public UnboundEncoder getFieldEncoder(RecordClassDescriptor rcd) {
        return encoder.getFieldEncoder(rcd);
    }

    @Override
    public void writeBinary(byte[] data, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    public void             beginWrite(MemoryDataOutput out) {
        if (encoder instanceof ContainerEncoder)
            ((ContainerEncoder)encoder).beginWrite(out);
    }

    public void             endWrite() {
        if (encoder instanceof ContainerEncoder)
            ((ContainerEncoder)encoder).endWrite();
    }

    @Override
    public void writeNull() {
        encoder.writeNull(ctx);
    }
}
