package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;

/**
 *  Executable stream selector.
 */
public final class BasicStreamSelector implements PreparedQuery {
    private final TickDB                db;
    private final TickStream []         streams;
    private final SelectionMode         mode;

    public BasicStreamSelector (
        SelectionMode               mode,
        TickStream []               streams
    )
    {
        this.db = streams [0].getDB ();

        for (int ii = 1; ii < streams.length; ii++)
            if (streams [ii].getDB () != db)
                throw new IllegalArgumentException (
                    "Mixing streams from multiple databases is not allowed."
                );
        
        this.streams = streams;        
        this.mode = mode;
    }

    public boolean                  isReverse () {
        return (mode == SelectionMode.REVERSE);
    }
    
    public InstrumentMessageSource  executeQuery (
        SelectionOptions                    options,
        ReadableValue []                    params
    ) 
    {
        if (options == null)
            options = new SelectionOptions ();
        
        switch (mode) {
            case NORMAL:
                options.reversed = false;
                options.live = false;
                break;
                
            case REVERSE:
                options.reversed = true;
                options.live = false;
                break;
                
            case LIVE:
                options.live = true;
                options.realTimeNotification = false;
                options.reversed = false;
                break;
                
            case HYBRID:
                options.live = true;
                options.realTimeNotification = true;
                options.reversed = false;
                break;
        }
        
        options.raw = true;
        
        return (db.createCursor (options, streams));
    }

    public void                     close () {       
    }
}
