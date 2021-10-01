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
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.TimeZone;

import static com.epam.deltix.timebase.messages.TimeStamp.NANOS_PER_MS;

/**
 * Formats date in UTC, not thread-safe.
 */
public class DateFormatter {

    private final FastDateFormat ticks = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.", TimeZone.getTimeZone("UTC"));
    private final FastDateFormat ms = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", TimeZone.getTimeZone("UTC"));
    private final DecimalFormat df = new DecimalFormat("000000000");
    private final StringBuffer buffer = new StringBuffer();

    private void format(StringBuilder builder, long milliseconds, int nanosComponent) {
        if (nanosComponent == 0 || milliseconds == Long.MIN_VALUE) {
            ms.format(milliseconds, builder);
        } else {
            ticks.format(milliseconds, builder);
            long ms = milliseconds % 1000;
            buffer.setLength(0);
            df.format(ms * NANOS_PER_MS + nanosComponent, buffer, new FieldPosition(0));
            builder.append(buffer).append('Z');
        }
    }

    public void toDateString(long milliseconds, StringBuilder sb) {
        format(sb, milliseconds, 0);
    }

    public String toDateString(long milliseconds) {
        StringBuilder sb = new StringBuilder();
        format(sb, milliseconds, 0);
        return sb.toString();
    }

    public void toDateString(long milliseconds, int nanosComponent, StringBuilder sb) {
        format(sb, milliseconds, nanosComponent);
    }

    public String toDateString(long milliseconds, int nanosComponent) {
        StringBuilder sb = new StringBuilder();
        format(sb, milliseconds, nanosComponent);
        return sb.toString();
    }

    public void toNanosDateString(long nanoTime, StringBuilder sb) {
        toDateString(TimeStamp.getMilliseconds(nanoTime), TimeStamp.getNanosComponent(nanoTime), sb);
    }

    public String toNanosDateString(long nanoTime) {
        StringBuilder sb = new StringBuilder();
        toNanosDateString(nanoTime, sb);
        return sb.toString();
    }

    public long fromDateString(String value) throws ParseException {
        return ms.parse(value).getTime();
    }

    public long fromNanosDateString(String value) throws ParseException {
        long secondsMs = ticks.parse(value).getTime();
        long nanos = df.parse(value, new ParsePosition(20)).longValue();
        long millis = nanos / NANOS_PER_MS;
        int nanosComponent = (int) (nanos % NANOS_PER_MS);
        return TimeStamp.getNanoTime(secondsMs + millis, nanosComponent);
    }

}