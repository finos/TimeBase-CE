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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeIntervalUtil {

    private TimeIntervalUtil() { }

    private static final Pattern PATTERN = Pattern.compile(
            "((?<days>\\d+)d)?((?<hours>\\d+)h)?((?<mins>\\d+)m)?((?<secs>\\d+)s)?((?<millis>\\d+)ms)?((?<nanos>\\d+)ns)?"
    );
    private static final Matcher MATCHER = PATTERN.matcher("");

    public static long parseMs(CharSequence value) {
        synchronized (MATCHER) {
            MATCHER.reset(value);
            if (MATCHER.matches()) {
                return days(MATCHER.group("days"))
                        + hours(MATCHER.group("hours"))
                        + minutes(MATCHER.group("mins"))
                        + seconds(MATCHER.group("secs"))
                        + millis(MATCHER.group("millis"));
            } else {
                throw new IllegalArgumentException(String.format("Argument '%s' does not match time interval pattern '%s'",
                        value, PATTERN.pattern()));
            }
        }
    }

    public static long parseNs(CharSequence value) {
        synchronized (MATCHER) {
            MATCHER.reset(value);
            if (MATCHER.matches()) {
                return toNanos(days(MATCHER.group("days")))
                        + toNanos(hours(MATCHER.group("hours")))
                        + toNanos(minutes(MATCHER.group("mins")))
                        + toNanos(seconds(MATCHER.group("secs")))
                        + toNanos(millis(MATCHER.group("millis")))
                        + nanos(MATCHER.group("nanos"));
            } else {
                throw new IllegalArgumentException(String.format("Argument '%s' does not match time interval pattern '%s'",
                        value, PATTERN.pattern()));
            }
        }
    }

    private static long days(@Nullable String value) {
        return interval(value, TimeUnit.DAYS);
    }

    private static long hours(@Nullable String value) {
        return interval(value, TimeUnit.HOURS);
    }

    private static long minutes(@Nullable String value) {
        return interval(value, TimeUnit.MINUTES);
    }

    private static long seconds(@Nullable String value) {
        return interval(value, TimeUnit.SECONDS);
    }

    private static long millis(@Nullable String value) {
        return interval(value, TimeUnit.MILLISECONDS);
    }

    private static long nanos(@Nullable String value) {
        return value == null ? 0: Long.parseLong(value);
    }

    private static long interval(@Nullable String value, TimeUnit unit) {
        return value == null ? 0: unit.toMillis(Long.parseLong(value));
    }

    private static long toNanos(long millis) {
        return millis * 1_000_000;
    }

}