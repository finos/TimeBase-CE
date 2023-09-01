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

import com.epam.deltix.util.collections.generated.*;
import java.util.zip.*;

/**
 *
 */
public class DeflateCompressor extends BlockCompressor {
    private final Deflater      deflater;

    public DeflateCompressor(int level, ByteArrayList buffer) {
        super(buffer);
        deflater = new Deflater (level);
    }

    @Override
    public byte                 code() {
        return BlockCompressorFactory.getCode(Algorithm.ZLIB);
    }

    public int                  deflate (
        byte []                     src, 
        int                         offset, 
        int                         length, 
        ByteArrayList               appendTo
    )
    {
        deflater.reset ();
        deflater.setInput (src, offset, length);
        deflater.finish ();
        
        int     numBytesOut = 0;
        
        while (!deflater.finished ()) {
            int         size = appendTo.size ();
            
            appendTo.ensureCapacity (size + 4096);
            
            byte []     data = appendTo.getInternalBuffer ();
            
            int         n = deflater.deflate (data, size, data.length - size);
            
            numBytesOut += n;
            
            appendTo.setSize (size + n);
        }
        
        return (numBytesOut);
    }
}