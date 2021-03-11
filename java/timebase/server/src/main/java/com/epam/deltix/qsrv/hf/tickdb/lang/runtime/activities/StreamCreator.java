package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.activities;

import com.epam.deltix.timebase.messages.service.ErrorLevel;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

/**
 *  Prepared query, which creates a stream.
 */
public class StreamCreator extends LoggingActivityLauncher {
    private final DXTickDB          db;
    private final String            key;
    private final StreamOptions     options;

    public StreamCreator (DXTickDB db, String key, StreamOptions options) {
        this.db = db;
        this.key = key;
        this.options = options;
    }
            
    @Override
    protected LoggingActivity       createActivity () {
        return (
            new LoggingActivity () {
                @Override
                protected void      run (ReadableValue[] params) {
                    db.createStream (key, options);  
                    log (SUCCESS, ErrorLevel.INFO, "Stream created", key);
                }                
            }
        );            
    }    
}
