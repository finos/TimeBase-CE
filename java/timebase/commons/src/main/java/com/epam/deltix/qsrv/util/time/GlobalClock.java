package com.epam.deltix.qsrv.util.time;

import com.epam.deltix.qsrv.hf.pub.TimeSource;

/**
 * @author Andy
 *         Date: Aug 12, 2010 9:59:10 AM
 */
public final class GlobalClock {

    private volatile TimeSource timeSource = RealTimeSource.INSTANCE;

    public static final GlobalClock INSTANCE = new GlobalClock();

    private GlobalClock () {}

    public long currentTimeMillis() {
        return timeSource.currentTimeMillis();
    }

    public void setTimeSource (TimeSource timeSource) {
        assert timeSource != null;
        this.timeSource = timeSource;
    }

    public TimeSource getTimeSource() {
        assert timeSource != null;
        return timeSource;
    }
}
