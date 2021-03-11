package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *
 */
public abstract class TimeConstants {
    public static final long    TIMESTAMP_UNKNOWN = Long.MIN_VALUE;
    public static final long    USE_CURRENT_TIME = Long.MIN_VALUE + 1;
    public static final long    USE_CURSOR_TIME = Long.MIN_VALUE + 2;
}
