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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.codecs.BlockDecompressor;
import com.epam.deltix.qsrv.dtb.store.dataacc.DataBlock;
import com.epam.deltix.util.collections.ByteArray;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

class FileInput {
    private long    offsetInFile = -1;

    private InputStream             is;
    private InputStream             bis;

    private volatile boolean        opened = false;
    private ByteArray               buffer;

    void        open(AbstractPath path, int offset) throws IOException {
        assert !opened;

        is = path.openInput (offset);
        bis = BufferedStreamUtil.wrapWithBuffered (is);

        opened = true;
        offsetInFile = offset;
    }

    public void         read(DataBlock into, int length, BlockDecompressor decompressor) throws IOException {
        ByteArray data = into.getData();

        if (decompressor == null) {
            IOUtil.readFully(bis, data.getArray(), data.getOffset(), into.getDataLength());
        } else if (length > 0) {
            if (buffer == null)
                buffer = new ByteArray(length);

            if (buffer.getLength() < length)
                buffer.setArray(new byte[length], 0, length); // @ALLOCATION

            int size = IOUtil.readFully(bis, buffer.getArray(), buffer.getOffset(), length);
            assert size == 0 : ("readFully (" + length + ") != 0");

            decompressor.inflate(buffer.getArray(), buffer.getOffset(), length, data.getArray(), data.getOffset(), into.getDataLength());
        }

        offsetInFile += length;
    }

    public boolean isOpened() {
        return opened;
    }

    void            seek (long offset) throws IOException {
        long    skip = offset - offsetInFile;

        if (skip < 0)
            throw new IllegalStateException (
                    "seek back from " + offsetInFile +
                            " to " + offset
            );

        if (skip > 0) {
            IOUtil.skipFully(bis, skip);
            offsetInFile = offset;
        }
    }

    boolean        close() {
        // inputs reused in pool - we have to clear all
        offsetInFile = -1;

        if (is != null) {
            Util.close(is);
            bis = null;
            is = null;
        }

        if (opened) {
            opened = false;
            return true;
        }

        return false;
    }

    public void setOffset(int offset) {
        this.offsetInFile = offset;
    }

    public long getOffsetInFile() {
        return offsetInFile;
    }

    /**
     * @return wrapped (buffered) input stream
     */
    public InputStream getInputStream() {
        return bis;
    }
}