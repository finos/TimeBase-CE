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
