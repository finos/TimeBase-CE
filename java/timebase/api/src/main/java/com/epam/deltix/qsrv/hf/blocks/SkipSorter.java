package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.SkipMessageException;

/**
 * Provides message processing rule that skips out-of-sequence messages.
 * Message is skipped, if message time is not exceeds previous message time by given amount of time (discrepancy).
 */

public class SkipSorter extends AbstractSorter<TimeIdentity> {
    private long            maxDiscrepancy;
    
    public SkipSorter(TimeIdentity id, MessageChannel<InstrumentMessage> channel, long maxDiscrepancy) {
        super(channel);
        this.entry = id;
        this.maxDiscrepancy = maxDiscrepancy;
    }

    @Override
    public void send(InstrumentMessage msg) {
        TimeIdentity key = getEntry(msg);
        final long timestamp = key.getTime();
        
        if (timestamp > msg.getTimeStampMs() && timestamp - msg.getTimeStampMs() <= maxDiscrepancy) {
            if (!ignoreErrors)
                onError(new SkipMessageException(msg, name));
        } else {
            prev.send(msg);
            key.setTime(msg.getTimeStampMs());
        }
    }   
}
