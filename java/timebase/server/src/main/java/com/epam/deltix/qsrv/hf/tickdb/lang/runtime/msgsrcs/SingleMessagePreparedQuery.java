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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.msgsrcs;

import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;

/**
 *  PreparedQuery which opens a SingleMessageEmitter.
 */
public final class SingleMessagePreparedQuery implements PreparedQuery {

    private static final RecordClassSet SCHEMA = new RecordClassSet(new RecordClassDescriptor[] {SingleMessageEmitter.VOID_TYPE});

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

    @Override
    public ClassSet<RecordClassDescriptor> getSchema() {
        return new RecordClassSet(SCHEMA);
    }
}