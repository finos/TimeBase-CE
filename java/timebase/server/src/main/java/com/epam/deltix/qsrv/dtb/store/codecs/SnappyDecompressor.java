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

import com.epam.deltix.qsrv.dtb.fs.alloc.ByteArrayHeap;
import com.epam.deltix.qsrv.dtb.fs.alloc.HeapManager;
import org.xerial.snappy.Snappy;

import java.io.IOException;

/**
 *
 */
public class SnappyDecompressor extends BlockDecompressor {

//    public SnappyDecompressor(ByteArrayHeap heap) {
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
        int infLength = 0;
        try {
            infLength = Snappy.uncompress(src, srcOffset, srcLength, out, outOffset);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }

        if (infLength != outLength)
            throw new com.epam.deltix.util.io.UncheckedIOException( "Inflated " + infLength + " bytes; expected: " + outLength
            );
    }
}