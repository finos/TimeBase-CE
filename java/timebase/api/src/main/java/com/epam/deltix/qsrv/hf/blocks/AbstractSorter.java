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
package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingError;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingErrorListener;


public abstract class AbstractSorter<T extends TimeIdentity>
        implements MessageChannel<InstrumentMessage>
{    
    protected final MessageChannel<InstrumentMessage> prev;
    protected LoadingErrorListener              listener;
    protected boolean                           ignoreErrors = true;
    protected String                            name;
    protected T                                 entry;
        
    protected AbstractSorter(MessageChannel<InstrumentMessage> channel) {
        this.prev = channel;
    }

    protected void              onError(LoadingError e) {
        if (listener != null)
            listener.onError(e);
        else
            throw e;
    }

    public String               getName() {
        return name;
    }

    public void                 setName(String name) {
        this.name = name;
    }

    public MessageChannel<InstrumentMessage> getChannel() {
        return prev;
    }

    public void                 setListener(LoadingErrorListener listener) {
        this.listener = listener;
    }

    public void                 setIgnoreErrors(boolean ignoreErrors) {
        this.ignoreErrors = ignoreErrors;
    }
    
    protected TimeIdentity      getEntry(InstrumentMessage msg) {
        return entry.get(msg);
    }   
    
    public void                 close() {
        if (prev != null)
            prev.close();
    }
}