package com.epam.deltix.qsrv.dtb.store.pub;

import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 *  Produces a message.
 */
public interface TSMessageProducer {    
    public void  writeBody (MemoryDataOutput out);
}