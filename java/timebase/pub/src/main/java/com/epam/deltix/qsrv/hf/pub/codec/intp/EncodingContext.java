package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public class EncodingContext extends CodecContext {
    public MemoryDataOutput        out;

    EncodingContext (RecordLayout layout) {
        super (layout);
    }        
}
