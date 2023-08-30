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

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.BumpUpMessageException;

 /**
 * Provides message processing rule that bump-up out-of-sequence messages. 
 */
public class BumpUpSorter extends AbstractSorter<TimeIdentity> {
    private long            maxDiscrepancy;
    
    public BumpUpSorter(TimeIdentity id, MessageChannel<InstrumentMessage> channel, long maxDiscrepancy) {
        super(channel);
        this.entry = id;
        this.maxDiscrepancy = maxDiscrepancy;
    }

    public void send(InstrumentMessage msg) {
        TimeIdentity key = getEntry(msg);
        final long timestamp = key.getTime();
        if (timestamp > msg.getTimeStampMs() && timestamp - msg.getTimeStampMs() <= maxDiscrepancy) {
            if (!ignoreErrors)
                onError(new BumpUpMessageException(msg, name, timestamp));
            msg.setTimeStampMs(timestamp);
        }
        prev.send(msg);
        key.setTime(msg.getTimeStampMs());
    }
}