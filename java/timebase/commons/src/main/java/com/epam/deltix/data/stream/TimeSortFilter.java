package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

/**
 *
 */
public class TimeSortFilter <T extends TimeStampedMessage>
    extends MessageFork <T>
{
    private long            lastTimestamp = Long.MIN_VALUE;
    
    public TimeSortFilter (
        MessageChannel<T> acceptedMessageConsumer,
        MessageChannel <T>  rejectedMessageConsumer
    )
    {
        super (acceptedMessageConsumer, rejectedMessageConsumer);
    }

    @Override
    protected boolean       accept (T message) {
        final long          ts = message.getTimeStampMs ();
        final boolean       ordered = ts >= lastTimestamp;

        if (ordered)
            lastTimestamp = ts;

        return (ordered);
    }
}
