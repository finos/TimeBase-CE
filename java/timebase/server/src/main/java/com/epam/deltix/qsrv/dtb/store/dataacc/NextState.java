package com.epam.deltix.qsrv.dtb.store.dataacc;

public class NextState {

    public static final int   NONE = 0;
    public static final int   HAS_CURRENT = 1;
    public static final int   HAS_MORE = 2;
    public static final int   HAS_ALL = 3;

    public static boolean  hasMore(int state) {
        return (state & HAS_MORE) == HAS_MORE;
    }

    public static boolean  hasCurrent(int state) {
        return (state & HAS_CURRENT) == HAS_CURRENT;
    }
}
