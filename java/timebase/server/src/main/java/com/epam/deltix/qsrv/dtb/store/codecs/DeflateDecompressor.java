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

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 *
 */
public class DeflateDecompressor extends BlockDecompressor {

    private final Inflater      inflater = new Inflater ();

//    public DeflateDecompressor(ByteArrayHeap heap) {
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
        inflater.reset ();
        inflater.setInput (src, srcOffset, srcLength);

        int         infLength;

        try {
            infLength = inflater.inflate (out, outOffset, outLength);
        } catch (DataFormatException x) {
            throw new com.epam.deltix.util.io.UncheckedIOException(x);
        }

        if (infLength != outLength)
            throw new com.epam.deltix.util.io.UncheckedIOException( "Inflated " + infLength + " bytes; expected: " + outLength
            );

        if (!inflater.finished ())
            throw new com.epam.deltix.util.io.UncheckedIOException( "Inflated " + infLength + " bytes but did not FINISH");
    }
}
