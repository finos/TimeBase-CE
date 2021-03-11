package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *
 */
public interface FixedUnboundEncoder extends UnboundEncoder, FixedCodec {
    public void                 beginWrite (MemoryDataOutput out);    
}
