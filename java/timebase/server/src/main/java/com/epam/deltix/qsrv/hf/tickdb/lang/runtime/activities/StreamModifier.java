/*
 * Copyright 2023 EPAM Systems, Inc
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

import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
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

    @Override
    public ClassSet<RecordClassDescriptor> getSchema() {
        ClassSet<RecordClassDescriptor> set = new RecordClassSet();
        set.addContentClasses(Messages.ERROR_MESSAGE_DESCRIPTOR);
        return set;
    }
}