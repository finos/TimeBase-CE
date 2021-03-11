package com.epam.deltix.util.io.idlestrat.adapter;

import com.epam.deltix.util.io.idlestrat.IdleStrategy;

/**
 * @author Alexei Osipov
 */
public final class IdleStrategyAdapter {
    public static IdleStrategy adapt(org.agrona.concurrent.IdleStrategy idleStrategy) {
        return ArgonaToDeltixStrategyAdapter.adapt(idleStrategy);
    }

    public static org.agrona.concurrent.IdleStrategy adapt(IdleStrategy idleStrategy) {
        return DeltixToArgonaStrategyAdapter.adapt(idleStrategy);
    }
}
