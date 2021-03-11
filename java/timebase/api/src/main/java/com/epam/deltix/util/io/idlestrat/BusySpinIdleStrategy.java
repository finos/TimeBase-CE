package com.epam.deltix.util.io.idlestrat;

import org.agrona.hints.ThreadHints;

/**
 * Aggressively burns CPU cycles. Use only if you need to get lowest possible latency and can afford dedicated
 * CPU core for each thread that uses this strategy.
 * <p>
 * Try using {@link YieldingIdleStrategy} before trying {@link BusySpinIdleStrategy}.
 *
 * @author Alexei Osipov
 */
public class BusySpinIdleStrategy implements IdleStrategy {
    @Override
    public void idle(int workCount) {
        if (workCount == 0) {
            idle();
        }
    }

    @Override
    public void idle() {
        ThreadHints.onSpinWait();
    }

    @Override
    public void reset() {
    }
}
