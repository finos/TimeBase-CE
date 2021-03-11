package com.epam.deltix.qsrv.snmp.modimpl;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.epam.deltix.util.time.GMT;

/**
 *
 */
public abstract class SnmpUtil {
    private static final String DATETIME_MILLIS_FORMAT_STR = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final SimpleDateFormat DTFX = new SimpleDateFormat(DATETIME_MILLIS_FORMAT_STR);

    static {
        DTFX.setTimeZone(GMT.TZ);
    }

    static String formatDateTimeMillis(long timestamp) {
        // yyyy-mm-dd hh:mm:ss.sss GMT
        if (timestamp > 0)
            synchronized (DTFX) {
                return DTFX.format(new Date(timestamp));
            }
        else
            return "";
    }
}
