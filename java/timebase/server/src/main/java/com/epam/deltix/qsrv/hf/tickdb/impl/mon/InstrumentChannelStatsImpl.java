package com.epam.deltix.qsrv.hf.tickdb.impl.mon;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.*;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.time.TimeKeeper;
import java.util.Date;

/**
 *
 */
public class InstrumentChannelStatsImpl
    extends ConstantIdentityKey
    implements InstrumentChannelStats
{
    private long                    numMessages = 0;
    private long                    lastTimestamp = Long.MIN_VALUE;
    private long                    lastSysTime = Long.MIN_VALUE;

    public InstrumentChannelStatsImpl (InstrumentChannelStatsImpl copy) {
        super (copy.getSymbol());

        numMessages = copy.numMessages;
        lastTimestamp = copy.lastTimestamp;
        lastSysTime = copy.lastSysTime;
    }

    public InstrumentChannelStatsImpl (IdentityKey copy) {
        super (copy.getSymbol());
    }

    public void             register (InstrumentMessage msg) {
        lastTimestamp = msg.getTimeStampMs();
        numMessages++;
        lastSysTime = TimeKeeper.currentTime;
    }

    public long             getLastMessageSysTime () {
        return (lastSysTime);
    }

    public long             getLastMessageTimestamp () {
        return (lastTimestamp);
    }

    public Date                         getLastMessageDate () {
        return lastTimestamp != Long.MIN_VALUE ? new Date (lastTimestamp) : null;
    }

    public Date                         getLastMessageSysDate () {
        return lastSysTime != Long.MIN_VALUE ? new Date (lastSysTime) : null;
    }

    public long             getTotalNumMessages () {
        return (numMessages);
    }
}
