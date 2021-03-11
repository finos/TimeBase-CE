package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 * A superclass for all exceptions, which might occur as a result of message loading ({@link deltix.data.stream.MessageChannel#send MessageChannel.send})
 */
public class LoadingMessageException extends LoadingError {
    protected final String symbol;
    protected long nanoTime;

    public LoadingMessageException(InstrumentMessage msg) {
        symbol = msg.getSymbol().toString();
        nanoTime = msg.getNanoTime();
    }
}
