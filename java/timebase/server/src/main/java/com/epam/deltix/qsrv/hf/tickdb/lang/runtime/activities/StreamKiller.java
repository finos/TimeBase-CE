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

import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
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

    @Override
    public ClassSet<RecordClassDescriptor> getSchema() {
        ClassSet<RecordClassDescriptor> set = new RecordClassSet();
        set.addContentClasses(Messages.ERROR_MESSAGE_DESCRIPTOR);
        return set;
    }
}