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

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *  Base class for generated predicates.
 */
public abstract class MessagePredicate {
    protected final RawMessage          msg = new RawMessage ();
    protected int                       typeIdx;
    
    protected abstract boolean          eval ();
    
    public final boolean                accept (
        InstrumentMessage                   msgHeader,
        int                                 typeIdx,
        byte []                             data,
        int                                 offset,
        int                                 length
    )
    {
        msg.setTimeStampMs(msgHeader.getTimeStampMs());
        msg.setNanoTime(msgHeader.getNanoTime());
        msg.setSymbol(msgHeader.getSymbol());
        msg.data = data;
        msg.offset = offset;
        msg.length = length;
        
        this.typeIdx = typeIdx;
        
        return (eval ());
    }
    
}