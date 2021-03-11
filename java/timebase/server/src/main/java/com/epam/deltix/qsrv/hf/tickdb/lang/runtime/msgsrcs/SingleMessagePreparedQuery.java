package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.msgsrcs;

import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;

/**
 *  PreparedQuery which opens a SingleMessageEmitter.
 */
public final class SingleMessagePreparedQuery implements PreparedQuery {    
    public SingleMessagePreparedQuery () {
        
    }

    public boolean                  isReverse () {
        return (false);
    }
    
    public InstrumentMessageSource  executeQuery (
        SelectionOptions                    options,
        ReadableValue []                    params
    ) 
    {
        return (new SingleMessageEmitter ());
    }

    public void                     close () {        
    }
}
