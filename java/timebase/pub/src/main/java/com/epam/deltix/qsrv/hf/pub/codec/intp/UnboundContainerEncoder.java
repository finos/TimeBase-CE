package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.FixedUnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.util.memory.MemoryDataOutput;

public class UnboundContainerEncoder implements ContainerEncoder {

    private final RecordTypeMap<RecordClassDescriptor>  map;
    private final FixedUnboundEncoder[]                 encoders;
    private MemoryDataOutput                            out;

    private final MemoryDataOutput                      local = new MemoryDataOutput();
    //private int                                         code = -1;

    public UnboundContainerEncoder(RecordClassDescriptor[] types) {
        map = new RecordTypeMap<> (types);
        encoders = new FixedUnboundEncoder[types.length];

        for (int i = 0; i < types.length; i++)
            encoders[i] = new FixedUnboundEncoderImpl(new RecordLayout(null, types[i]));
    }

    @Override
    public void                         beginWrite(MemoryDataOutput out) {
        this.out = out;
        this.local.reset();
    }

    @Override
    public void                         endWrite() {
        int size = local.getSize();
        MessageSizeCodec.write(size, out);
        out.write(local.getBuffer(), 0, size);
    }

    public FixedUnboundEncoder          getEncoder(RecordClassDescriptor type) {
        int code = map.getCode(type);
        FixedUnboundEncoder encoder = encoders[code];
        encoder.beginWrite(local);

        local.writeUnsignedByte(code);

        return encoder;
    }
}
