package com.epam.deltix.qsrv.hf.tickdb.impl.mon;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.time.TimeKeeper;
import java.util.Collections;
import java.util.Date;
import net.jcip.annotations.GuardedBy;

/**
 *
 */
public class InstrumentChannelStatSet implements ChannelStats {
    @GuardedBy ("items")
    private final ObjectArrayList <InstrumentChannelStatsImpl>       items =
            new ObjectArrayList<>();

    @GuardedBy ("items")
    private long                    numMessages = 0;

    @GuardedBy ("items")
    private long                    lastTimestamp = Long.MIN_VALUE;

    @GuardedBy ("items")
    private long                    lastSysTime = Long.MIN_VALUE;

    private final TBMonitor         parent;

    public InstrumentChannelStatSet (TBMonitor parent) {
        this.parent = parent;
    }

    public void                         clear () {
        synchronized (items) {
            items.clear ();
        }
    }

    public void                         register (InstrumentMessage msg) {
        if (parent == null || !parent.getTrackMessages ())
            return;

        registerInternal(msg);
    }

    private void registerInternal(InstrumentMessage msg) {
        synchronized (items) {
            int                     pos =
                Collections.binarySearch (
                    items,
                    msg,
                    IdentityKeyComparator.DEFAULT_INSTANCE
                );

            InstrumentChannelStatsImpl        item;

            if (pos >= 0) 
                item = items.get (pos);
            else {
                item = new InstrumentChannelStatsImpl (msg);
                
                items.add (-pos - 1, item);
            }

            item.register (msg);

            lastTimestamp = msg.getTimeStampMs();
            numMessages++;
            lastSysTime = TimeKeeper.currentTime;
        }
    }

    public InstrumentChannelStats []    getInstrumentStats () {
        synchronized (items) {
            int                         n = items.size ();
            InstrumentChannelStats []   ret = new InstrumentChannelStats [n];
            
            for (int ii = 0; ii < n; ii++)
                ret [ii] = new InstrumentChannelStatsImpl (items.get (ii));

            return (ret);
        }
    }

    public long                         getLastMessageSysTime () {
        return (lastSysTime);
    }

    public long                         getLastMessageTimestamp () {
        return (lastTimestamp);
    }

    public Date                         getLastMessageDate () {
        return lastTimestamp != Long.MIN_VALUE ? new Date (lastTimestamp) : null;
    }

    public Date                         getLastMessageSysDate () {
        return lastSysTime != Long.MIN_VALUE ? new Date (lastSysTime) : null;
    }

    public long                         getTotalNumMessages () {
        return (numMessages);
    }
}
