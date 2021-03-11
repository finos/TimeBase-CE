package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.activities;

import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.StreamChangeTask;
import com.epam.deltix.timebase.messages.service.ErrorLevel;

/**
 *  Prepared query, which modifies a stream.
 */
public class StreamModifier extends LoggingActivityLauncher {
    private final DXTickDB              db;
    private final String                key;
    private final StreamChangeTask      changeTask;

    public StreamModifier (DXTickDB db, String key, StreamChangeTask changes) {
        this.db = db;
        this.key = key;
        this.changeTask = changes;
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
                        s.execute (changeTask);
                        log (SUCCESS, ErrorLevel.INFO, "Stream modification task was started", key);
                    }
                }                
            }
        );
    }    
}
