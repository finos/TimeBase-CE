package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.time.GMT;

/**
 * Notifies a client that timestamp of an out-of-sequence message was bumped up.
 */
public class BumpUpMessageException extends LoadingMessageException {
    private String stream;
    private final long newTimestamp;

    public BumpUpMessageException(InstrumentMessage msg, String stream, long newTimestamp) {
        super(msg);
        this.stream = stream;
        this.newTimestamp = newTimestamp;
    }

    @Override
    public String getMessage() {
        return String.format("[%s] Message %s %s GMT is bumped up to %s GMT",
                stream, symbol, GMT.formatNanos(nanoTime), GMT.formatDateTimeMillis(newTimestamp));
    }
}
