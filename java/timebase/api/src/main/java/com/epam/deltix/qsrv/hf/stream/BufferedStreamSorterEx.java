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
package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.data.stream.BufferedStreamSorter;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *
 */
public class BufferedStreamSorterEx <T extends InstrumentMessage>
    extends BufferedStreamSorter <T>
{
    private boolean             copyMessages = true;
    
    public BufferedStreamSorterEx (MessageChannel <T> delegate, long width) {
        super (delegate, width);
    }

    public BufferedStreamSorterEx (MessageChannel <T> delegate, long width, int capacity) {
        super (delegate, width, capacity);
    }

    public boolean              getCopyMessages () {
        return copyMessages;
    }

    /**
     *  Determines whether this object should automatically create a copy of
     *  all messages supplied to {@link #send}. Set this flag to false if the
     *  messages can be taken over. Set this flag to true if the messages will
     *  be re-used by the caller.
     *
     *  @param copyMessages     Whether this object should automatically 
     *                          copy all messages.
     */
    public void                 setCopyMessages (boolean copyMessages) {
        this.copyMessages = copyMessages;
    }

    @Override @SuppressWarnings ("unchecked")
    public void                 send (T msg) {
        if (copyMessages)
            msg = (T) msg.clone();

        super.send (msg);
    }
}