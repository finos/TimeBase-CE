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
package com.epam.deltix.qsrv.util.json;

import com.epam.deltix.timebase.messages.TimeStamp;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.assertEquals;

public class DateFormatterTest {

    @Test
    public void testNanos() throws ParseException {
        long millis = System.currentTimeMillis();
        int nanos = 273278;
        long nanoTime = TimeStamp.getNanoTime(millis, nanos);
        testNanos(nanoTime);
    }

    @Test
    public void testMillis() throws ParseException {
        long millis = System.currentTimeMillis();
        testMillis(millis);
    }

    private void testNanos(long nanoTime) throws ParseException {
        DateFormatter dateFormatter = new DateFormatter();
        String formatted = dateFormatter.toNanosDateString(nanoTime);
        long parsed = dateFormatter.fromNanosDateString(formatted);
        assertEquals(String.format("Formatted: %s", formatted), parsed, nanoTime);
    }

    private void testMillis(long millis) throws ParseException {
        DateFormatter dateFormatter = new DateFormatter();
        String formatted = dateFormatter.toDateString(millis);
        long parsed = dateFormatter.fromDateString(formatted);
        assertEquals(String.format("Formatted: %s", formatted), parsed, millis);
    }

}