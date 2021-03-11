package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *  Forces server-side setting of the timestamp of all messages
 *  passing through this loader, by resetting the timestamp to
 *  {@link TimeConstants#TIMESTAMP_UNKNOWN}
 */
public class TimestampIgnoringTickLoader extends FilterTickLoader<InstrumentMessage> {

    public TimestampIgnoringTickLoader (TickLoader<InstrumentMessage> delegate) {
        super (delegate);
    }

    @Override
    public void         send (InstrumentMessage msg) {
        msg.setTimeStampMs(TimeConstants.TIMESTAMP_UNKNOWN);
        super.send (msg);
    }
}
