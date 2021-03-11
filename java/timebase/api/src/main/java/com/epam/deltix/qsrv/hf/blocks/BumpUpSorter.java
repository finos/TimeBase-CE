package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.BumpUpMessageException;

 /**
 * Provides message processing rule that bump-up out-of-sequence messages. 
 */
public class BumpUpSorter extends AbstractSorter<TimeIdentity> {
    private long            maxDiscrepancy;
    
    public BumpUpSorter(TimeIdentity id, MessageChannel<InstrumentMessage> channel, long maxDiscrepancy) {
        super(channel);
        this.entry = id;
        this.maxDiscrepancy = maxDiscrepancy;
    }

    public void send(InstrumentMessage msg) {
        TimeIdentity key = getEntry(msg);
        final long timestamp = key.getTime();
        if (timestamp > msg.getTimeStampMs() && timestamp - msg.getTimeStampMs() <= maxDiscrepancy) {
            if (!ignoreErrors)
                onError(new BumpUpMessageException(msg, name, timestamp));
            msg.setTimeStampMs(timestamp);
        }
        prev.send(msg);
        key.setTime(msg.getTimeStampMs());
    }
}
