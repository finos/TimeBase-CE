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
package com.epam.deltix.data.stream.pq.utilityclasses;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

import java.util.Random;

/**
 * @author Alexei Osipov
 */
public class TimeStampedMessageMessageSource implements MessageSource<TimeStampedMessage> {
    private final Random random;
    private final int step;
    private final InstrumentMessage message = new InstrumentMessage();
    private final long multiplier;

    public TimeStampedMessageMessageSource(long baseTimestampForSource, Random random, int step, long multiplier) {
        this.multiplier = multiplier;
        this.message.setNanoTime(baseTimestampForSource);

        this.random = random;
        this.step = step;
    }

    @Override
    public InstrumentMessage getMessage() {
        return message;
    }

    @Override
    public boolean next() {
        long prevTime = message.getNanoTime();
        long newTime = getNextRandomTime(prevTime, random, step, multiplier);
        message.setNanoTime(newTime);
        return true;
    }

    @Override
    public boolean isAtEnd() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public String toString() {
        return "TSM{" +
                "ts=" + message.getNanoTime() +
                '}';
    }

    public static long getNextRandomTime(long prevTime, Random random, int step) {
        return prevTime + random.nextInt(step);  // Value 0 is included
    }

    public static long getNextRandomTime(long prevTime, Random random, int step, long multiplier) {
        return prevTime + random.nextInt(step) * multiplier;  // Value 0 is included
    }
}