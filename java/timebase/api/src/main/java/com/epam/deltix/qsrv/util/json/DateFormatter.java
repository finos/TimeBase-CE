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

import org.apache.commons.lang3.time.FastDateFormat;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Formats date in GMT, not thread-safe.
 */
public class DateFormatter {
    public static final String DATETIME_MILLIS_FORMAT_STR = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private final Calendar mCalendar;
    private final FastDateFormat DTFX = FastDateFormat.getInstance(DATETIME_MILLIS_FORMAT_STR, TimeZone.getTimeZone("UTC"));

    public DateFormatter() {
        mCalendar = new GregorianCalendar();
        mCalendar.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void toDateString(long timestamp, StringBuilder sb) {
        mCalendar.setTimeInMillis(timestamp);
        DTFX.format(mCalendar, sb);
    }

    public long fromDateString(String value) throws ParseException {
        return DTFX.parse(value).getTime();
    }
}
