/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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