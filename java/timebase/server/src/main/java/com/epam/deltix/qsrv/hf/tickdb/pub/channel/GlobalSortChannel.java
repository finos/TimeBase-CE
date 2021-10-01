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
package com.epam.deltix.qsrv.hf.tickdb.pub.channel;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.stream.MessageSorter;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Util;
import java.io.IOException;

/**
 *
 */
public final class GlobalSortChannel
    implements MessageChannel<InstrumentMessage>
{
    private final MessageSorter                         sorter;
    private final MessageChannel <InstrumentMessage>    downstream;

    // stats for getProgress function
    private volatile long           num = 0;

    public GlobalSortChannel (
        long                                memory,
        MessageChannel <InstrumentMessage>  downstream,
        LoadingOptions                      options,
        RecordClassDescriptor ...           descriptors
    )
    {
        this.downstream = downstream;
        
        sorter = 
            new MessageSorter (
                memory,
                options.raw ? null : options.getTypeLoader (),
                descriptors
            );
    }

    public void                 send (InstrumentMessage msg) {
        try {
            sorter.add (msg);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    public void                 close () {
        MessageSource<InstrumentMessage> cur;

        try {
            cur = sorter.finish (null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            while (cur.next ()) {
                downstream.send (cur.getMessage ());
                num++;
            }
        } finally {
            Util.close (cur);
            Util.close (downstream);
            sorter.close ();
        }
    }

    public double               getProgress () {
        return (double) num / sorter.getTotalNumMessages ();
    }
}