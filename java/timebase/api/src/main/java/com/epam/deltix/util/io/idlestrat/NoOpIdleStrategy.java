package com.epam.deltix.util.io.idlestrat;

/**
 * @deprecated Use {@link BusySpinIdleStrategy} instead.
 */
public class NoOpIdleStrategy implements IdleStrategy {
    @Override
    public void idle(int workCount) {
    }

    @Override
    public void idle() {
    }

    @Override
    public void reset() {
    }
}
