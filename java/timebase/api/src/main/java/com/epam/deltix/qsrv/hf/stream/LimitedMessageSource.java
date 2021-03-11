package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *  MessageSource that simulates end-of-stream at given timestamp
*/
public class LimitedMessageSource implements MessageSource<InstrumentMessage> {

    private MessageSource<InstrumentMessage> delegate;
    private final long limit;
    private boolean keepReading = true;

    public LimitedMessageSource(MessageSource<InstrumentMessage> delegate, long limit) {
        this.delegate = delegate;
        this.limit = limit;
    }

    @Override
    public InstrumentMessage getMessage() {
        assert keepReading;

        return delegate.getMessage();
    }

    @Override
    public boolean isAtEnd() {
        return ! keepReading;
    }

    @Override
    public boolean next() {
        if (keepReading) {
            keepReading = delegate.next();
            if (keepReading) {
                InstrumentMessage msg = delegate.getMessage();
                keepReading = (msg.getTimeStampMs() < limit);
            }
        }
        return keepReading;
    }

    @Override
    public void close() {
        delegate.close();
    }
}
