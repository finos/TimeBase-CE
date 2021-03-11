package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *
 */
public class NestedObjectCodec {
    public final static int     NULL_CODE = 0xFF;
    
    public static void          skip (MemoryDataInput in) {
        int     size = MessageSizeCodec.read (in);
        in.skipBytes (size);
    }
}
