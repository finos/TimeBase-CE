package com.epam.deltix.util.io.waitstrat;

import com.epam.deltix.util.lang.Changeable;

/**
 *
 */
public class YieldingWaitForChangeStrategy implements WaitForChangeStrategy {
    public YieldingWaitForChangeStrategy() {
    }

    public void                 waitFor(Changeable value) {
        while (!value.changed()) {
            Thread.yield();
        }
    }

    public void                 close() {
    }
}
