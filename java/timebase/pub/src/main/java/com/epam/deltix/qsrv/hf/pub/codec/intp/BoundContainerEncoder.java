package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.util.memory.MemoryDataOutput;

public class BoundContainerEncoder {

    private final RecordTypeMap<Class>      map;
    private final FixedBoundEncoder[]       encoders;

    private final MemoryDataOutput          local = new MemoryDataOutput();
    private final TypeLoader                loader;
    private final RecordClassDescriptor[]   types;

    public BoundContainerEncoder(TypeLoader loader, RecordClassDescriptor[] types) {
        this.loader = loader;
        this.types = types;

        Class<?>[] classes = new Class<?>[types.length];

        encoders = new FixedBoundEncoder[types.length];
        for (int ii = 0; ii < types.length; ii++) {
            RecordLayout layout = new RecordLayout(loader, types[ii]);
            layout.setEmbedded();
            classes[ii] = layout.getTargetClass();
        }

        map = new RecordTypeMap<Class>(classes);
    }

    void writeObject(Object value, EncodingContext ctx) {

        if (value == null) {
            writeNull(ctx);
        } else {
            local.reset();

            int code = map.getCode(value.getClass());
            local.writeUnsignedByte(code);

            FixedBoundEncoder encoder = encoders[code];
            if (encoder == null)
                encoder = encoders[code] = new FixedBoundEncoderImpl(new RecordLayout(loader, types[code]));
            encoder.encode(value, local);

            int size = local.getSize();
            MessageSizeCodec.write(size, ctx.out);
            ctx.out.write(local.getBuffer(), 0, size);
        }
    }

    public void writeNull(EncodingContext ctx) {
        MessageSizeCodec.write(0, ctx.out);
    }
}
