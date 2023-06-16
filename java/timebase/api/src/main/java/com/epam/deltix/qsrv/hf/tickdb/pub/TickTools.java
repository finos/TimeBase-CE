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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.progress.ProgressIndicator;

public class TickTools {
    public static long []   getTimeRange (
        TickStream ...          streams
    )
    {
        long []         tr = { Long.MAX_VALUE, Long.MIN_VALUE };

        for (TickStream stream : streams) {
            long []     str = stream.getTimeRange ();

            if (str != null) {
                if (str [0] < tr [0])
                    tr [0] = str [0];

                if (str [1] > tr [1])
                    tr [1] = str [1];
            }
        }

        if (tr [0] == Long.MAX_VALUE)
            return (null);

        return (tr);
    }
    
    public static void                  copy (
        MessageSource<InstrumentMessage>    ac,
        long []                             timeRange,
        MessageChannel<InstrumentMessage> channel,
        ProgressIndicator                   pi
    )
    {
        long    nextReport = 0;
        long    reportInterval = 0;
        long    width = timeRange [1] - timeRange [0];

        if (pi != null) {
            reportInterval = width / 1000;
            nextReport = reportInterval;

            pi.setTotalWork (width);
        }

        while (ac.next ()) {
            InstrumentMessage   msg = (InstrumentMessage) ac.getMessage ();

            final long        t = msg.getTimeStampMs();

            if (t > timeRange [1])
                break;

            channel.send (msg);

            if (pi != null) {
                if (t >= nextReport) {
                    pi.setWorkDone (t - timeRange [0]);
                    nextReport = t + reportInterval;
                }
            }
        }

        if (pi != null)
            pi.setWorkDone (width);
    }
}