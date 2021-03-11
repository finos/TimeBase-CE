package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *
 */
public interface BoundExternalDecoder {

    /**
     *  Sets static values into specified message.
     *  @param message  A message object to update.
     */
    public void                 setStaticFields (
        Object                      message
    );

    /**
     *  Decodes an object, starting at current position of the
     *  specified MemoryDataInput.
     *
     *  @param in       A MemoryDataInput to read.
     *  @param message  A message object to decode into.
     */
    public void                 decode (
        MemoryDataInput             in,
        Object                      message
    );
}
