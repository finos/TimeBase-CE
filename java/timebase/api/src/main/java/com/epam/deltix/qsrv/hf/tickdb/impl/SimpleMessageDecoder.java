package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.BoundExternalDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

public class SimpleMessageDecoder {

    private final BoundExternalDecoder      decoder;
    private final InstrumentMessage         message;
    private final RecordClassDescriptor     type;
    private final MemoryDataInput           input;

    public SimpleMessageDecoder (
            TypeLoader                  loader,
            CodecFactory                factory,
            RecordClassDescriptor       fixedType
    )
    {
        type = fixedType;
        message = (InstrumentMessage) fixedType.newInstanceNoX(loader);

        decoder = factory.createFixedExternalDecoder(loader, fixedType);
        decoder.setStaticFields (message);
        input = new MemoryDataInput();
    }

    public InstrumentMessage decode(MemoryDataInput in, int length) {

        input.setBytes(in.getBytes(), in.getCurrentOffset(), length);
        decoder.decode(input, message);
        return message;
    }

    public InstrumentMessage getMessage() {
        return message;
    }

    public RecordClassDescriptor getType() {
        return type;
    }
}
