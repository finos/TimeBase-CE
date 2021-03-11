package com.epam.deltix.util.io.idlestrat;

/**
 * Provides good latency without fully overloading CPU core like {@link BusySpinIdleStrategy}.
 */
public class YieldingIdleStrategy implements IdleStrategy {
    @Override
    public void idle(int workCount) {
        if (workCount == 0) {
            idle();
        }
    }

    @Override
    public void idle() {
        Thread.yield();
    }

    @Override
    public void reset() {
        // Do nothing
    }
}
