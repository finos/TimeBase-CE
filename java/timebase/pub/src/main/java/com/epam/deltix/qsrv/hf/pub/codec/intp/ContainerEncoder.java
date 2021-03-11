package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.util.memory.MemoryDataOutput;

public interface ContainerEncoder {

    void beginWrite(MemoryDataOutput out);

    void endWrite();
}