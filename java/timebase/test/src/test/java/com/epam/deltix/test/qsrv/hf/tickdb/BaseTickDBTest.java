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
package com.epam.deltix.test.qsrv.hf.tickdb;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageSource;
import org.junit.Assert;

import com.epam.deltix.util.collections.generated.LongArrayList;

public class BaseTickDBTest {

    static final String [] ENTITIES = { "MSFT", "IBM", "ORCL", ".IXIC" };

    static final DateFormat DATE_FORMAT = new SimpleDateFormat ("yyyy-MM-dd");

    final Random rnd = new Random (152);


    static class TestTickMessageSource implements MessageSource<TradeMessage> {

        private final Random rnd = new Random (49);
        private final long TIME_STEP = TimeUnit.DAYS.toMillis(7);

        private final long fromTime;
        private long toTime;
        private long timeIncrement;

        private long messageTime;
        private boolean lastTimeFrame;
        private int generationNo = 0;

        private int symbolIdx = -1;
        private final LongArrayList messageTimes = new LongArrayList (10000);

        public TestTickMessageSource(long fromTime, long toTime) {
            this (fromTime, toTime, -1);
        }

        public TestTickMessageSource(long fromTime, long toTime, long timeIncrement) {
            Assert.assertTrue (fromTime <= toTime);
            this.fromTime = fromTime;
            this.timeIncrement = timeIncrement;
            init (fromTime, toTime);
        }

        public void prepareForAppend (long fromTime, long toTime) {
            Assert.assertTrue (fromTime <= toTime);
            Assert.assertTrue (this.fromTime <= fromTime);

            if (this.toTime > fromTime) {
                messageTimes.trimToSize();
                // TicDB will truncate all existing messages in DB from fromTime
                long [] times = messageTimes.getInternalBuffer();
                int index = Arrays.binarySearch(times, fromTime);
                if (index < 0) {
                    index = -index-1;
                }
                if (index < times.length)
                    messageTimes.setSize(index);
            }
            init(fromTime, toTime);
        }

        public void init (long fromTime, long toTime) {
            this.toTime = toTime;
            messageTime = fromTime;
            symbolIdx = -1;
            messageTimes.add(messageTime);
            generationNo++;
            lastTimeFrame = false;
        }

        @Override
        public TradeMessage getMessage() {
            TradeMessage result = new TradeMessage ();
            result.setTimeStampMs(messageTime);
            result.setSymbol(ENTITIES[symbolIdx]);
            result.setSize(generationNo);
            return result;
        }

        @Override
        public boolean isAtEnd() {
            return messageTime > toTime;
        }

        @Override
        public boolean next() {
            if (++symbolIdx == ENTITIES.length) {
                symbolIdx = 0;

                if (timeIncrement > 0)
                    messageTime +=timeIncrement;
                else
                    messageTime += 1 + (long) ((TIME_STEP)*rnd.nextDouble());
                if (messageTime > toTime) {
                    if (lastTimeFrame)
                        return false;

                    messageTime = toTime;
                }

                messageTimes.add(messageTime);
                lastTimeFrame = (messageTime == toTime);
            }


            return true;
        }

        @Override
        public void close() {
            messageTimes.trimToSize();
        }

        long getClosestTimeAfter (long randomTime) {
            long [] generatedTimes = messageTimes.getInternalBuffer();
            int index = Arrays.binarySearch(generatedTimes, randomTime);
            if (index < 0) {
                index = -index-1;
                if (index >= generatedTimes.length)
                    index = generatedTimes.length - 1;
            }

            return generatedTimes [index];
        }

        /** @return time in range [FROM..TO] controlled by Random rnd */
        long nextRandomTime (Random rnd) {
            return fromTime + (long) ((getActualToTime() - getActualFromTime())*rnd.nextDouble());
        }

        long getActualFromTime () {
            return messageTimes.getLong(0);
        }

        long getActualToTime () {
            return messageTimes.getLong(messageTimes.size() - 1);
        }

        long getMidpoint () {
            return messageTimes.getLong(messageTimes.size() / 2);
        }

    }

//    void assertTimesMatch (TickCursor cursor, FeedFilter filter, long requestedTime, long expectedTime) {
//        assertTimesMatch (cursor, filter, requestedTime, expectedTime, 1);
//    }
//
//
//    void assertTimesMatch (TickCursor cursor, FeedFilter filter, long requestedTime, long expectedTime, int generationNo) {
//        CharSequenceSet symbols = filter.equityFilter.getSymbols ();
//        final int size = (symbols != null) ? symbols.size() : 0;
//        for (int i=0; i < size; i++) {
//            Assert.assertTrue ("TICK DB must have some data for " + formatDateTime(requestedTime), cursor.next());
//            TradeMessage msg = (TradeMessage) cursor.getMessage();
//            long actualTime = msg.getTimeStampMs();
//            Assert.assertEquals ("TICK DB was expected to retrieve message with timestamp " + formatDateTime(expectedTime) + ", but actual time was " + formatDateTime(actualTime), expectedTime, actualTime);
//            Assert.assertTrue("accepted symbol", symbols.containsCharSequence (msg.getSymbol()));
//
//            // we use TradeMessage.size to store generationNo
//            Assert.assertEquals(generationNo, (int) msg.getSize());
//        }
//    }




    static long parseDate (String date) {
        try {
            return DATE_FORMAT.parse(date).getTime();
        } catch (ParseException e) {
            throw new RuntimeException (e);
        }
    }
}
