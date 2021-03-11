package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *
 */
public interface BoundDecoder extends BoundExternalDecoder {
    /**
     *  Decodes an object, starting at current position of the 
     *  specified MemoryDataInput.
     * 
     *  @param in       A MemoryDataInput to read.
     *  @return         A decoded message object.
     */
    public Object               decode (MemoryDataInput in);
}
