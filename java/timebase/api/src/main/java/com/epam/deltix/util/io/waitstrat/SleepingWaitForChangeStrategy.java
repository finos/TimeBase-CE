package com.epam.deltix.util.io.waitstrat;

import com.epam.deltix.util.lang.Changeable;

import java.util.concurrent.locks.LockSupport;

/**
 *
 */
public class SleepingWaitForChangeStrategy implements WaitForChangeStrategy {
    private int                 count = 1000;

    public SleepingWaitForChangeStrategy() {
    }

    public void                 waitFor(Changeable value) {
        while (!value.changed()) {
            if (count > 500) {
                --count;
            } else if (count > 0) {
                --count;
                Thread.yield();
            } else {
                LockSupport.parkNanos(1);
            }
        }

        count = 1000;
    }

    public void                 close() {
    }
}
