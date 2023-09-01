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
package com.epam.deltix.qsrv.hf.tickdb.impl.mon;

import com.epam.deltix.qsrv.hf.tickdb.pub.mon.*;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.time.TimeKeeper;
import java.util.Date;

/**
 *
 */
public class InstrumentChannelStatsImpl
    extends ConstantIdentityKey
    implements InstrumentChannelStats
{
    private long                    numMessages = 0;
    private long                    lastTimestamp = Long.MIN_VALUE;
    private long                    lastSysTime = Long.MIN_VALUE;

    public InstrumentChannelStatsImpl (InstrumentChannelStatsImpl copy) {
        super (copy.getSymbol());

        numMessages = copy.numMessages;
        lastTimestamp = copy.lastTimestamp;
        lastSysTime = copy.lastSysTime;
    }

    public InstrumentChannelStatsImpl (IdentityKey copy) {
        super (copy.getSymbol());
    }

    public void             register (InstrumentMessage msg) {
        lastTimestamp = msg.getTimeStampMs();
        numMessages++;
        lastSysTime = TimeKeeper.currentTime;
    }

    public long             getLastMessageSysTime () {
        return (lastSysTime);
    }

    public long             getLastMessageTimestamp () {
        return (lastTimestamp);
    }

    public Date                         getLastMessageDate () {
        return lastTimestamp != Long.MIN_VALUE ? new Date (lastTimestamp) : null;
    }

    public Date                         getLastMessageSysDate () {
        return lastSysTime != Long.MIN_VALUE ? new Date (lastSysTime) : null;
    }

    public long             getTotalNumMessages () {
        return (numMessages);
    }
}