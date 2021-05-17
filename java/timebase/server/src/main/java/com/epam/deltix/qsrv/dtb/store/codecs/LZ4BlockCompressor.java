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
package com.epam.deltix.qsrv.dtb.store.codecs;

import com.epam.deltix.util.collections.generated.ByteArrayList;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

/**
 *
 */
public class LZ4BlockCompressor extends BlockCompressor {
    private final LZ4Compressor               compressor;

    public LZ4BlockCompressor(int compressionLevel, ByteArrayList buffer) {
        super(buffer);

        if (compressionLevel == 1)
            compressor = LZ4Factory.fastestInstance().fastCompressor();
        else
            compressor = LZ4Factory.fastestInstance().highCompressor(compressionLevel - 1);
    }

    @Override
    public byte                 code() {
        return BlockCompressorFactory.getCode(Algorithm.LZ4);
    }

    public int                  deflate (
        byte []                     src,
        int                         offset,
        int                         length,
        ByteArrayList               appendTo
    )
    {
        int maxCompressedLength = compressor.maxCompressedLength(length);

        int size = appendTo.size();
        appendTo.ensureCapacity(size + maxCompressedLength);
        byte[] data = appendTo.getInternalBuffer();
        int compressedLength = compressor.compress(src, offset, length, data, size, maxCompressedLength);
        appendTo.setSize(size + compressedLength);

        return compressedLength;
    }
}
