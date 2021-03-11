package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *  Base class for generated predicates.
 */
public abstract class MessagePredicate {
    protected final RawMessage          msg = new RawMessage ();
    protected int                       typeIdx;
    
    protected abstract boolean          eval ();
    
    public final boolean                accept (
        InstrumentMessage msgHeader,
        int                                 typeIdx,
        byte []                             data,
        int                                 offset,
        int                                 length
    )
    {
        msg.setTimeStampMs(msgHeader.getTimeStampMs());
        msg.setNanoTime(msgHeader.getNanoTime());
        msg.setSymbol(msgHeader.getSymbol());
        msg.data = data;
        msg.offset = offset;
        msg.length = length;
        
        this.typeIdx = typeIdx;
        
        return (eval ());
    }
    
}
