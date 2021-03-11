package com.epam.deltix.qsrv.hf.tickdb.impl;


import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *
 */
class LossyMessageQueueWriter extends MessageQueueWriter <LossyMessageQueue> {

    LossyMessageQueueWriter (
        TransientStreamImpl                         stream,
        LossyMessageQueue                           queue,
        MessageEncoder<InstrumentMessage>           encoder
    )
    {
        super (stream, queue, encoder);
    }

    public void       send (InstrumentMessage msg) {
        if (prepare (msg))
            writeBuffer(msg.getTimeStampMs());
    }
}
