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
package com.epam.deltix.qsrv.dtb.store.codecs;

import com.epam.deltix.util.collections.generated.ByteArrayList;
import org.xerial.snappy.Snappy;

import java.io.IOException;

/**
 *
 */
public class SnappyCompressor extends BlockCompressor {

    public SnappyCompressor(ByteArrayList buffer) {
        super(buffer);
    }

    @Override
    public byte                 code() {
        return BlockCompressorFactory.getCode(Algorithm.SNAPPY);
    }

    @Override
    public int deflate(byte[] src, int offset, int length, ByteArrayList appendTo) {
        int maxCompressedLength = Snappy.maxCompressedLength(length);
        int size = appendTo.size();
        appendTo.ensureCapacity(size + maxCompressedLength);
        byte[] data = appendTo.getInternalBuffer();
        int compressedLength = 0;
        try {
            compressedLength = Snappy.compress(src, offset, length, data, size);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }

        appendTo.setSize(size + compressedLength);

        return compressedLength;
    }
}