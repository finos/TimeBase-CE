package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.activities;

import com.epam.deltix.timebase.messages.service.ErrorLevel;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

/**
 *  Prepared query, which deletes a stream.
 */
public class StreamKiller extends LoggingActivityLauncher {
    private final DXTickDB          db;
    private final String            key;
    
    public StreamKiller (DXTickDB db, String key) {
        this.db = db;
        this.key = key;
    }
            
    @Override
    protected LoggingActivity       createActivity () {
        return (
            new LoggingActivity () {
                @Override
                protected void      run (ReadableValue[] params) {
                    DXTickStream    s = db.getStream (key);  
                    
                    if (s == null)
                        log (NOTFOUND, ErrorLevel.USER_ERROR, "Stream not found", key);
                    else {
                        s.delete ();                    
                        log (SUCCESS, ErrorLevel.INFO, "Stream dropped", key);
                    }
                }                
            }
        );            
    }    
}
