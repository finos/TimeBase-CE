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

import com.epam.deltix.qsrv.dtb.fs.alloc.ByteArrayHeap;
import com.epam.deltix.qsrv.dtb.fs.alloc.HeapManager;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;


/**
 *
 */
public class LZ4BlockDecompressor extends BlockDecompressor {
    private final LZ4Factory                    factory = LZ4Factory.fastestInstance();
    private final LZ4FastDecompressor           decompressor = factory.fastDecompressor();

    public LZ4BlockDecompressor() {
    }

//    public LZ4BlockDecompressor(ByteArrayHeap heap) {
//        super(heap);
//    }

    public void                 inflate (
        byte []                     src,
        int                         srcOffset,
        int                         srcLength,
        byte []                     out,
        int                         outOffset,
        int                         outLength
    )
    {
        int compressedLength = decompressor.decompress(src, srcOffset, out, outOffset, outLength);
        if (compressedLength != srcLength)
            throw new com.epam.deltix.util.io.UncheckedIOException( "Compressed size " + compressedLength + " bytes; expected: " + srcLength
            );
    }
}
