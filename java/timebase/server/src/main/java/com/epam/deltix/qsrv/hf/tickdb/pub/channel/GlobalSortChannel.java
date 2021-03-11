package com.epam.deltix.qsrv.hf.tickdb.pub.channel;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.stream.MessageSorter;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Util;
import java.io.IOException;

/**
 *
 */
public final class GlobalSortChannel
    implements MessageChannel<InstrumentMessage>
{
    private final MessageSorter                         sorter;
    private final MessageChannel <InstrumentMessage>    downstream;

    // stats for getProgress function
    private volatile long           num = 0;

    public GlobalSortChannel (
        long                                memory,
        MessageChannel <InstrumentMessage>  downstream,
        LoadingOptions                      options,
        RecordClassDescriptor ...           descriptors
    )
    {
        this.downstream = downstream;
        
        sorter = 
            new MessageSorter (
                memory,
                options.raw ? null : options.getTypeLoader (),
                descriptors
            );
    }

    public void                 send (InstrumentMessage msg) {
        try {
            sorter.add (msg);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    public void                 close () {
        MessageSource<InstrumentMessage> cur;

        try {
            cur = sorter.finish (null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            while (cur.next ()) {
                downstream.send (cur.getMessage ());
                num++;
            }
        } finally {
            Util.close (cur);
            Util.close (downstream);
            sorter.close ();
        }
    }

    public double               getProgress () {
        return (double) num / sorter.getTotalNumMessages ();
    }
}
