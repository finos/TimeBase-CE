package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.qsrv.hf.pub.codec.CharSequenceDecoder;
import com.epam.deltix.util.memory.MemoryDataInput;

public class CharSequencePool extends ObjectPool<StringBuilder> implements CharSequenceDecoder {

    public CharSequencePool() {
        super(1);
    }

    @Override
    public StringBuilder newItem() {
        return new StringBuilder();
    }

    public CharSequence      readCharSequence(MemoryDataInput in) {
        StringBuilder builder = borrow();
        builder.setLength(0);

        return in.readStringBuilder(builder);
    }
}
