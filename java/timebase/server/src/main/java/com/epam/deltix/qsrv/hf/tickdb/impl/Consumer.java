package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *
 */
public class Consumer extends MessageConsumer<InstrumentMessage> {

    private final SimpleMessageDecoder[]    decoders;

    public Consumer (RegistryCache registry, RecordClassDescriptor[] types, TypeLoader loader, CodecFactory factory, boolean realTimeNotification) {
        super(registry, types, realTimeNotification);

        this.decoders = new SimpleMessageDecoder[types.length];
        for (int i = 0; i < types.length; i++)
            decoders[i] = new SimpleMessageDecoder(loader, factory, types[i]);
    }

    @Override
    public void     process (int entity, long timestampNanos, int type, int bodyLength, MemoryDataInput mdi) {
        SimpleMessageDecoder decoder = decoders[type];

        currentTypeIndex = type;

        message = decoder.decode(mdi, bodyLength);

        message.setNanoTime(timestampNanos);
        registry.decode(message, entity);
    }

    @Override
    public RecordClassDescriptor getCurrentType() {
        if (currentTypeIndex == _REALTIME_MESSAGE_TYPE_INDEX)
            return Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR;

        return decoders[currentTypeIndex].getType();
    }

    public InstrumentMessage            getMessage() {
        return message;
    }

    @Override
    protected InstrumentMessage         makeRealTimeStartMessage(long timestampNanos) {
        final RealTimeStartMessage msg = new RealTimeStartMessage();
        msg.setSymbol("");
        msg.setNanoTime(timestampNanos);
        return msg;
    }

}