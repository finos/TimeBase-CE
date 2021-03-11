package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.RecordDecoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Filter;
import com.epam.deltix.util.memory.MemoryDataInput;

public class PolyBoundDecoder implements RecordDecoder<InstrumentMessage> {

    private SimpleBoundDecoder []       decoders;
    private RecordClassDescriptor[]     types;

    private int code = -1;

    public PolyBoundDecoder (
            TypeLoader loader,
            CodecFactory factory,
            RecordClassDescriptor []        types
    )
    {
        final int                       num = types.length;

        if (num > 256)
            throw new IllegalArgumentException (
                    "Too many classes: " + types.length + " (max 256)"
            );

        this.decoders = new SimpleBoundDecoder [num];
        this.types = types;

        for (int ii = 0; ii < num; ii++)
            decoders [ii] = new SimpleBoundDecoder (types [ii], loader, factory);
    }

    public InstrumentMessage            decode (
            Filter<? super InstrumentMessage> filter,
            MemoryDataInput in
    )
    {
        code = decoders.length > 1 ? in.readUnsignedByte () : 0;
        return (decoders [code].decode (filter, in));
    }

    @Override
    public RecordClassDescriptor        getCurrentType() {
        return decoders[code].getCurrentType();
    }

    @Override
    public int                          getCurrentTypeIndex() {
        return code;
    }
}

