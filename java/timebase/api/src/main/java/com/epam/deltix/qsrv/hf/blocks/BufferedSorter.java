package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.TimestampPriorityQueue;
import com.epam.deltix.timebase.messages.TimeStampedMessage;


/**
 * Provides message processing rule that sorts out-of-sequence messages below in given period of time. 
 */

public class BufferedSorter extends AbstractSorter<TimeIdentity> {
    private long            maxDiscrepancy;       
    
    public BufferedSorter(Entry id,
                          MessageChannel<InstrumentMessage> channel,
                          long maxDiscrepancy) {
        super(channel);
        this.entry = id;
        this.maxDiscrepancy = maxDiscrepancy;
    }

     public BufferedSorter(TimeIdentitySet<Entry> id,
                          MessageChannel<InstrumentMessage> channel,
                          long maxDiscrepancy) {
        super(channel);
        this.entry = id;
        this.maxDiscrepancy = maxDiscrepancy;
    }

    public void send(InstrumentMessage msg) {
        Entry key = (Entry) getEntry(msg);
        
        final long timestamp = msg.getTimeStampMs();
        long maxTimestampSeen = key.getTime();
        
        final TimestampPriorityQueue<InstrumentMessage> queue = key.queue;

        if (timestamp > maxTimestampSeen) {
            key.setTime(timestamp);

            final long          limit = timestamp - maxDiscrepancy;

            //  Flush messages that are too old
            for (;;) {
                final InstrumentMessage top = queue.peek ();

                if (top == null)
                    break;

                final long      topTS = top.getTimeStampMs();

                if (topTS >= limit)
                    break;

                final InstrumentMessage check = queue.poll ();

                if (check != top)
                    throw new RuntimeException ("peek != poll");

                prev.send (top);
            }
        } else if (maxTimestampSeen > maxDiscrepancy + timestamp) {
            prev.send(msg);                
            return;
        }

        queue.offer (msg.clone());
    }

    public void                 flush () {
        if (entry instanceof Iterable) {
            for (Object o : ((Iterable) entry))
                flush(((Entry) o).queue);
        } else {
            flush(((Entry) entry).queue);
        }
    }

    private void            flush (TimestampPriorityQueue<InstrumentMessage> queue) {
        for (;;) {
            InstrumentMessage msg = queue.poll ();

            if (msg == null)
                break;

            prev.send (msg);
        }
    }

    public void             close () {
        flush ();
        super.close();
    }

    public static class Entry extends TimeEntry {
        private final TimestampPriorityQueue<InstrumentMessage> queue =
            new TimestampPriorityQueue<InstrumentMessage>(64);

        public Entry() {
        }

        public Entry(IdentityKey id, long timestamp) {
            super(id, timestamp);
        }

        @Override
        public TimeIdentity create(IdentityKey id) {
            return new Entry(id, TimeStampedMessage.TIMESTAMP_UNKNOWN);
        }
    }
}
