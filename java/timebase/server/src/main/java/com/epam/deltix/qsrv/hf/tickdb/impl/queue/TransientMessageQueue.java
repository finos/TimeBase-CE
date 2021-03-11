package com.epam.deltix.qsrv.hf.tickdb.impl.queue;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.QuickMessageFilter;

/**
 * @author Alexei Osipov
 */
public interface TransientMessageQueue {
    QueueMessageReader getMessageReader(QuickMessageFilter filter, boolean polymorphic, boolean realTimeNotification);

    MessageChannel<InstrumentMessage> getWriter(MessageEncoder<InstrumentMessage> encoder);

    void close();
}
