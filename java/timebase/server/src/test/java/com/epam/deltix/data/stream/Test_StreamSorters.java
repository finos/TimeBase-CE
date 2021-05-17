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
package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.UHFUtils;

/**
 *
 */
@Category(Object.class)
public class Test_StreamSorters {
    private static final class Msg implements TimeStampedMessage {
        final long            ts;

        public Msg (long ts) {
            this.ts = ts;
        }

        public long getTimeStampMs() {
            return (ts);
        }

        @Override
        public long getNanoTime() {
            return TimeStamp.getNanoTime(ts);
        }

        @Override
        public String   toString () {
            return ("Test Message @" + ts);
        }
    }

    @Test
    public void         unitTestBufferedSorter () {
        BufferedStreamSorter <TimeStampedMessage>    s =
            new BufferedStreamSorter <TimeStampedMessage> (
                new MessageChannel<TimeStampedMessage>() {
                    private long        expect = 10;

                    public void         send (TimeStampedMessage msg) {
                        assertEquals (expect, msg.getTimeStampMs());
                        expect += 10;
                    }

                    public void         close () {
                        assertEquals (100, expect);
                    }
                },
                20
            );

        s.send (new Msg (10));
        s.send (new Msg (30));

        assertEquals (30 * TimeStamp.NANOS_PER_MS, s.getMaxTime());
        assertEquals (0, s.getMaxViolation ());
        
        s.send (new Msg (20));  // out of order by -10

        assertEquals (30 * TimeStamp.NANOS_PER_MS, s.getMaxTime());
        assertEquals (10 * TimeStamp.NANOS_PER_MS, s.getMaxViolation ());

        s.send (new Msg (40));

        try {
            s.send (new Msg (19));
            assertTrue ("Failed to crash on message too old", false);
        } catch (IllegalArgumentException x) {
            if (!Boolean.getBoolean ("quiet"))
                System.out.println ("(This is good): " + x.getMessage ());
            // The only acceptable result
        }

        s.send (new Msg (60));
        s.send (new Msg (70));

        //  Barely makes it...
        s.send (new Msg (50));

        assertEquals (70 * TimeStamp.NANOS_PER_MS, s.getMaxTime());
        assertEquals (20 * TimeStamp.NANOS_PER_MS, s.getMaxViolation ());

        s.send (new Msg (80));
        s.send (new Msg (90));

        s.close ();
    }

    @Test
    public void         stressTestBufferedSorter () {
        Meter <Msg>         meter = new Meter <Msg> (null);

        BufferedStreamSorter <Msg>    s =
            new BufferedStreamSorter <Msg> (
                new TimeSortFilter <Msg> (
                    meter,
                    new ExceptionMessageChannel <Msg> ()
                ),
                100
            );

        long                mean = System.currentTimeMillis ();
        Random              rnd = new Random (2009);

        for (int ii = 0; ii < 100000; ii++) {
            s.send (new Msg (mean + rnd.nextInt (101)));
            mean++;
        }

        s.close ();

        assertEquals (100000, meter.getCount ());
    }
}
