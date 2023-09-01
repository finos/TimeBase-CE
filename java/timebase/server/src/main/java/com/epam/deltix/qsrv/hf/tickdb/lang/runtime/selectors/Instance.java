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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Variable holding an object value.
 */
public class Instance {
    protected static final byte[] NULL_BUFFER = new byte[0];
    protected static final int NULL = 0;
    protected static final int NULL_TYPE = -1;

    private byte[] buffer = NULL_BUFFER;
    private int length = NULL;
    private int offset = NULL;
    private int typeId = NULL_TYPE;

    private final MemoryDataOutput out = new MemoryDataOutput();

    public Instance() {
    }

    public byte[] bytes() {
        return buffer;
    }

    public int length() {
        return length;
    }

    protected void setLength(int length) {
        this.length = length;
    }

    public int offset() {
        return offset;
    }

    public int typeId() {
        return typeId;
    }

    public void typeId(int typeId) {
        this.typeId = typeId;
    }

    public boolean isNull() {
        return (length == NULL);
    }

    public void adjustTypeId(int[] adjustTypeIndex) {
        if (typeId != NULL_TYPE) {
            for (int i = 0; i < adjustTypeIndex.length; i += 2) {
                if (this.typeId == adjustTypeIndex[i]) {
                    this.typeId = adjustTypeIndex[i + 1];
                    break;
                }
            }
        }
    }

    public void set(Instance other) {
        if (other == null) {
            reset();
        } else {
            this.typeId = other.typeId;
            this.buffer = other.bytes();
            this.length = other.length;
            this.offset = other.offset;
        }
    }

    public void copyFrom(Instance other) {
        if (other == null) {
            reset();
        } else {
            this.typeId = other.typeId;
            this.buffer = new byte[other.length];
            System.arraycopy(other.bytes(), 0, this.buffer, 0, other.length);
            this.length = other.length;
            this.offset = other.offset;
        }
    }

    public void set(int typeId, MemoryDataInput mdi) {
        this.typeId = typeId;
        this.buffer = mdi.getBytes();
        this.length = mdi.getLength();
        this.offset = mdi.getCurrentOffset();
    }

    protected void set(int typeId, MemoryDataOutput out) {
        this.typeId = typeId;
        this.buffer = out.getBuffer();
        this.length = out.getSize();
        this.offset = 0;
    }

    public void reset() {
        buffer = NULL_BUFFER;
        length = NULL;
        offset = NULL;
        typeId = NULL_TYPE;
    }

    public void decode(MemoryDataInput mdi) {
        final int size = MessageSizeCodec.read(mdi);

        assert size >= 0;

        final int length = size > 0 ? size - 1 /* typeId byte */ : size;
        if (size > 0) {
            out.reset();
            out.ensureSize(length);
            int typeId = mdi.readByte();
            mdi.readFully(out.getBuffer(), 0, length);
            out.skip(length);
            set(typeId, out);
        } else {
            reset();
        }
    }

    public void encode(MemoryDataOutput out) {
        if (length == NULL) { // no message
            MessageSizeCodec.write(0, out);
        } else {
            MessageSizeCodec.write(length + 1 /* typeId byte */, out);
            if (length > 0) {
                out.writeByte(typeId);
                out.write(buffer, offset, length);
            }
        }
    }

    protected void decodeArray(MemoryDataInput mdi) {
        final int length = MessageSizeCodec.read(mdi);
        if (length > 0) {
            out.reset();
            out.ensureSize(length);
            mdi.readFully(out.getBuffer(), 0, length);
            out.skip(length);
            set(NULL_TYPE, out);
        } else {
            reset();
        }
    }

    protected void encodeArray(MemoryDataOutput out) {
        if (length == NULL) { // no message
            MessageSizeCodec.write(0, out);
        } else {
            MessageSizeCodec.write(length, out);
            if (length > 0) {
                out.write(bytes(), offset(), length());
            }
        }
    }

    public static boolean equals(Instance i1, Instance i2) {
        return i1.length == i2.length && i1.typeId == i2.typeId &&
            (i1.isNull() || equals(i1.buffer, i1.offset, i2.buffer, i2.offset, i1.length));
    }

    private static boolean equals(byte[] buf1, int offset1, byte[] buf2, int offset2, int length) {
        for (int i = 0; i < length; i++) {
            if (buf1[offset1 + i] != buf2[offset2 + i]) {
                return false;
            }
        }
        return true;
    }
}