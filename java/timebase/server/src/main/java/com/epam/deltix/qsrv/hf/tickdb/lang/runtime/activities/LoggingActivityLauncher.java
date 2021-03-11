package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.activities;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.msgsrcs.RawMessagePipe;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.timebase.messages.service.ErrorLevel;
import com.epam.deltix.timebase.messages.service.ErrorMessage;


/**
 *
 */
public abstract class LoggingActivityLauncher implements PreparedQuery {
    
    public static abstract class LoggingActivity {
        protected static final String          SUCCESS = "SUCCESS";
        protected static final String          NOTFOUND = "NOTFOUND";
        
        private final RawMessagePipe    logPipe = new RawMessagePipe ("query-log#");        
        private ErrorMessage            errMessage = null;
        
        protected abstract void         run (ReadableValue [] params);
        
        private void                    execute (ReadableValue [] params) {
            try {
                run (params);
            } catch (Exception x) {
                log (x);
            } finally {
                logPipe.writer.close ();
            }                
        }    
        
        protected final void            log (Throwable x) {
            x = Util.unwrap (x);
            
            log (
                x.getClass ().getName (),
                ErrorLevel.USER_ERROR,
                x.getMessage (),
                Util.printStackTrace (x)
            );
        }
        
        protected final void            log (
            String                          type,
            ErrorLevel                      level,
            String                          messageText,
            String                          details
        )
        {
            if (errMessage == null) {
                errMessage = new ErrorMessage ();
                errMessage.setSymbol("");
            }
            
            errMessage.setErrorType(type);
            errMessage.setLevel(level);
            errMessage.setMessageText(messageText);
            //errMessage.details = details;     
            
            send (errMessage);
        }
        
        protected final void            send (InstrumentMessage msg) {
            logPipe.writer.send (msg);
        }
    }

    private boolean                         isOpen = true;
    
    
    protected abstract LoggingActivity      createActivity ();
    
    public final InstrumentMessageSource    executeQuery (
        SelectionOptions                options,
        ReadableValue []                params
    )
    {    
        if (!isOpen)
            throw new IllegalStateException ("closed");
        
        LoggingActivity         task = createActivity ();
        
        task.execute (params);
        
        return (task.logPipe.reader);
    }
    
    public final boolean                    isReverse () {
        return (false);
    }

    protected void                          onClose () {        
    }
    
    public final void                       close () {
        if (isOpen) {
            onClose ();
            isOpen = false;
        }
    }    
}
