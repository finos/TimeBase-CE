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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.qsrv.hf.pub.md.SimpleClassSet;

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
                options.live = false;
                options.reversed = false;
                options.realTimeNotification = false;
                break;
                
            case REVERSE:
                options.live = false;
                options.reversed = true;
                options.realTimeNotification = false;
                break;
                
            case LIVE:
                options.live = true;
                options.realTimeNotification = false;
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

    @Override
    public ClassSet<RecordClassDescriptor> getSchema() {
        return new SimpleClassSet(Streams.catTypes(streams));
    }

    public void                     close () {
    }
}