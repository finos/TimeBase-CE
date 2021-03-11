package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.qsrv.hf.pub.md.*;
/**
 *
 */
public interface PolyUnboundEncoder extends UnboundEncoder {
    public void                 beginWrite (
        RecordClassDescriptor       rcd, 
        MemoryDataOutput            out
    );    
}
