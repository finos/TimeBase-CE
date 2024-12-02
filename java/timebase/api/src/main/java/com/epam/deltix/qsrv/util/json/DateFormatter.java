/*
 * Copyright 2024 EPAM Systems, Inc
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
    private final DecimalFormat nanoDf = new DecimalFormat("000000000");
    private final DecimalFormat mkDf = new DecimalFormat("000000");
    private final DecimalFormat msDf = new DecimalFormat("000");
    private final StringBuffer buffer = new StringBuffer();

    private void format(StringBuilder builder, long milliseconds, int nanosComponent) {
        if (nanosComponent == 0 || milliseconds == Long.MIN_VALUE) {
            formatMs(builder, milliseconds);
        } else {
            formatNano(builder, milliseconds, nanosComponent);
        }
    }

    private void formatMs(StringBuilder builder, long milliseconds) {
        ms.format(milliseconds, builder);
    }

    private void formatNano(StringBuilder builder, long milliseconds, int nanosComponent) {
        ticks.format(milliseconds, builder);
        long ms = milliseconds % 1000;
        buffer.setLength(0);
        nanoDf.format(ms * NANOS_PER_MS + nanosComponent, buffer, new FieldPosition(0));
        builder.append(buffer).append('Z');
    }

    private void formatNanoPretty(StringBuilder builder, long milliseconds, int nanosComponent) {
        ticks.format(milliseconds, builder);
        long ms = milliseconds % 1000;
        buffer.setLength(0);
        long nns = ms * NANOS_PER_MS + nanosComponent;
        if (nns % 1000000 == 0) {
            msDf.format(nns / 1000000, buffer, new FieldPosition(0));
        } else if (nns % 1000 == 0) {
            mkDf.format(nns / 1000, buffer, new FieldPosition(0));
        } else {
            nanoDf.format(nns, buffer, new FieldPosition(0));
        }
        builder.append(buffer).append('Z');
    }

    public void toDateString(long milliseconds, StringBuilder sb) {
        formatMs(sb, milliseconds);
    }

    public String toDateString(long milliseconds) {
        StringBuilder sb = new StringBuilder();
        formatMs(sb, milliseconds);
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

    public void appendNanosDateString(long nanoTime, StringBuilder sb) {
        formatNano(sb, TimeStamp.getMilliseconds(nanoTime), TimeStamp.getNanosComponent(nanoTime));
    }

    public void appendDateString(long nanoTime, StringBuilder sb) {
        formatNanoPretty(sb, TimeStamp.getMilliseconds(nanoTime), TimeStamp.getNanosComponent(nanoTime));
    }

    public String toNanosDateString(long nanoTime) {
        StringBuilder sb = new StringBuilder();
        toNanosDateString(nanoTime, sb);
        return sb.toString();
    }

    public long msFromDateString(String value) throws ParseException {
        return ms.parse(value).getTime();
    }

    public long nanoFromDateString(String value) throws ParseException {
        long msPart = ticks.parse(value).getTime();
        String nanos = value.substring(20, value.length() - 1);
        if (nanos.length() > 9) {
            nanos = nanos.substring(0, 9);
        }
        int nsValue = Integer.parseInt(nanos);
        if (nanos.length() == 9) { // full nanoseconds
            return TimeStamp.getNanoTime(msPart, nsValue);
        } else if (nanos.length() == 3) { // milliseconds
            return TimeStamp.getNanoTime(msPart + nsValue);
        } else {
            double multiplier = Math.max(0, 9 - nanos.length());
            nsValue = nsValue * (int) Math.pow(10, multiplier);
            return TimeStamp.getNanoTime(msPart, nsValue);
        }
    }

}