package com.epam.deltix.util.io.idlestrat.adapter;

import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Adapts {@link org.agrona.concurrent.IdleStrategy} to {@link IdleStrategy} interface.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class ArgonaToDeltixStrategyAdapter implements deltix.util.io.idlestrat.IdleStrategy {
    private final org.agrona.concurrent.IdleStrategy wrapped;

    static IdleStrategy adapt(org.agrona.concurrent.IdleStrategy idleStrategy) {
        // Try to find direct match and use "native" implementation if possible.
        Class<? extends org.agrona.concurrent.IdleStrategy> sClass = idleStrategy.getClass();
        if (sClass.equals(BusySpinIdleStrategy.class)) {
            return new BusySpinIdleStrategy();
        } else if (sClass.equals(YieldingIdleStrategy.class)) {
            return new YieldingIdleStrategy();
        } else if (sClass.equals(DeltixToArgonaStrategyAdapter.class)) {
            return ((DeltixToArgonaStrategyAdapter) idleStrategy).getWrapped();
        } else {
            // Fallback to proxy-based adapter
            return new ArgonaToDeltixStrategyAdapter(idleStrategy);
        }
    }

    org.agrona.concurrent.IdleStrategy getWrapped() {
        return wrapped;
    }

    private ArgonaToDeltixStrategyAdapter(org.agrona.concurrent.IdleStrategy wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void idle(int workCount) {
        wrapped.idle(workCount);
    }

    @Override
    public void idle() {
        wrapped.idle();
    }

    @Override
    public void reset() {
        wrapped.reset();
    }
}
