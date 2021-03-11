package com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue;

import com.epam.deltix.qsrv.hf.tickdb.impl.QuickMessageFilter;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.ByteRingBuffer;

/**
 * @author Alexei Osipov
 */
final public class DisruptorQueueReaderLossless extends DisruptorQueueReader {
    DisruptorQueueReaderLossless(DisruptorMessageQueue disruptorMessageQueue, ByteRingBuffer ringBuffer, QuickMessageFilter filter, boolean polymorphic, boolean realTimeNotification) {
        super(disruptorMessageQueue, ringBuffer, filter, polymorphic, realTimeNotification);
    }

    @Override
    boolean pageDataIn() {
        boolean dataLoaded;
        if (isAsynchronous()) {
            dataLoaded = asyncPageIn();
        } else {
            dataLoaded = syncPageIn();
        }

        // We always have data in live mode
        assert dataLoaded || !isLive();

        // Publish all we have
        sequence.set(consumedSequence);
        return dataLoaded;
    }
}
