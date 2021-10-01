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
package com.epam.deltix.qsrv.dtb.fs.cache;

import com.epam.deltix.util.collections.ByteArray;

import java.io.IOException;
import java.io.OutputStream;

class CachingOutputStream extends OutputStream {

    private final OutputStream delegate;
    private final byte [] buffer;
    private final int base;
    private int bytesStored;
    private final int length;

    protected CachingOutputStream(OutputStream delegate, byte [] buffer, int offset, int length) {
        this.delegate = delegate;
        this.buffer = buffer;
        this.base = offset;
        this.length = length;
    }

    public CachingOutputStream(OutputStream os, ByteArray buffer) {
        this(os, buffer.getArray(), buffer.getOffset(), buffer.getLength());
    }

    @Override
    public void write(int b) throws IOException {
        if (bytesStored > length)
            throw new ArrayIndexOutOfBoundsException(bytesStored);

        buffer[base + bytesStored] = (byte) b;
        bytesStored++;

        delegate.write(b);
    }

    @Override
    public void write(byte[] block) throws IOException {
        write(block, 0, block.length);
    }

    @Override
    public void write(byte[] block, int blockOffset, int blockLength) throws IOException {
        if (this.bytesStored + blockLength > this.length)
            throw new ArrayIndexOutOfBoundsException(this.bytesStored);

        System.arraycopy(block, blockOffset, buffer, this.base + this.bytesStored, blockLength);
        this.bytesStored += blockLength;

        delegate.write(block, blockOffset, blockLength);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

}