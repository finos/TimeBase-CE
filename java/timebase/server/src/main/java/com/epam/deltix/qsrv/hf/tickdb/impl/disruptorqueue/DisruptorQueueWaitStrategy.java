package com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue;

import com.lmax.disruptor.*;

/**
 * @author Alexei Osipov
 */
final class DisruptorQueueWaitStrategy implements WaitStrategy {
    private final DisruptorMessageQueue queue;
    private final WaitStrategy wrappedStrategy;

    public DisruptorQueueWaitStrategy(DisruptorMessageQueue queue, WaitStrategy wrappedStrategy) {
        this.queue = queue;
        this.wrappedStrategy = wrappedStrategy;
    }

    @Override
    public long waitFor(long sequence, Sequence cursor, Sequence dependentSequence, SequenceBarrier barrier) throws AlertException, InterruptedException, TimeoutException {
        return wrappedStrategy.waitFor(sequence, cursor, dependentSequence, barrier);
    }

    @Override
    public void signalAllWhenBlocking() {
        queue.notifyWaitingReaders();
        wrappedStrategy.signalAllWhenBlocking();
    }
}
