package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.RecordDecoder;
import com.epam.deltix.qsrv.hf.tickdb.pub.CommonOptions;
import com.epam.deltix.timebase.messages.InstrumentMessage;

public abstract class MessageCodec {

    public static RecordDecoder<InstrumentMessage> createDecoder(RecordClassDescriptor[] types, CommonOptions options) {
        if (options.raw)
            return new SimpleRawDecoder(types);

        boolean compiled = options.channelQOS == ChannelQualityOfService.MAX_THROUGHPUT;

        return new PolyBoundDecoder(options.getTypeLoader(), CodecFactory.get(compiled), types);
    }
}
