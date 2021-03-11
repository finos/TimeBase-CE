package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Disposable;

/**
 * Something that can provide messages to given target channel
 */
public interface MessageSender extends Disposable {
    void send (MessageChannel<InstrumentMessage> target);
}
