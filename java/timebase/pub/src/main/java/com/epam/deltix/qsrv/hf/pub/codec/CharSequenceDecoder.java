package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.util.memory.MemoryDataInput;

public interface CharSequenceDecoder {

    CharSequence readCharSequence(MemoryDataInput in);

}
