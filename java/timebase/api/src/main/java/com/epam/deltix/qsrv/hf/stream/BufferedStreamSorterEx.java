package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.data.stream.BufferedStreamSorter;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *
 */
public class BufferedStreamSorterEx <T extends InstrumentMessage>
    extends BufferedStreamSorter <T>
{
    private boolean             copyMessages = true;
    
    public BufferedStreamSorterEx (MessageChannel <T> delegate, long width) {
        super (delegate, width);
    }

    public BufferedStreamSorterEx (MessageChannel <T> delegate, long width, int capacity) {
        super (delegate, width, capacity);
    }

    public boolean              getCopyMessages () {
        return copyMessages;
    }

    /**
     *  Determines whether this object should automatically create a copy of
     *  all messages supplied to {@link #send}. Set this flag to false if the
     *  messages can be taken over. Set this flag to true if the messages will
     *  be re-used by the caller.
     *
     *  @param copyMessages     Whether this object should automatically 
     *                          copy all messages.
     */
    public void                 setCopyMessages (boolean copyMessages) {
        this.copyMessages = copyMessages;
    }

    @Override @SuppressWarnings ("unchecked")
    public void                 send (T msg) {
        if (copyMessages)
            msg = (T) msg.clone();

        super.send (msg);
    }
}
