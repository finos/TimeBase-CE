package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.qsrv.hf.pub.TimeSource;

/**
 * Clock that just show chosen time.
 * To change time you have to do that manually.
 *
 * @author Alexei Osipov
 */
// TODO: Move this class to time-related utility classes.
public class ManualClock implements TimeSource {
    // Note: this field should not be updated from multiple threads without proper synchronization.
    private volatile long currentTimeMillis = 0;

    @Override
    public long currentTimeMillis() {
        return currentTimeMillis;
    }

    public void setCurrentTimeMillis(long currentTimeMillis) {
        this.currentTimeMillis = currentTimeMillis;
    }

    public void advanceByMillis(long timeIntervalMs) {
        this.currentTimeMillis += timeIntervalMs;
    }
}
