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

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *
 */
public class PolyBoundDecoderImpl implements BoundDecoder {
    private BoundDecoder []         decoders;
    
    public PolyBoundDecoderImpl (BoundDecoder [] decoders) {
        this.decoders = decoders; 
    }

    protected final BoundDecoder    getDecoder (int code) {
        BoundDecoder    decoder = decoders [code];

        if (decoder == null)
            throw new RuntimeException (
                "Decoder for class #" + code +
                " was not created (probably due to unloadable class)"
            );

        return (decoder);
    }

    public Object           decode (MemoryDataInput in) {
        int             code = in.readUnsignedByte ();        
        
        return (getDecoder (code).decode (in));
    }

    public void             decode (MemoryDataInput in, Object message) {
        int             code = in.readUnsignedByte ();        
        
        getDecoder (code).decode (in, message);
    }

    public void             setStaticFields (Object message) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }    
}
