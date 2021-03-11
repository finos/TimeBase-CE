package com.epam.deltix.qsrv.hf.tickdb.impl;

/**
 *
 */
class EmptyReader<T extends MessageQueue> extends MessageQueueReader <T> {

    public EmptyReader(TickStreamImpl stream) {
        super(stream);
    }

    @Override
    protected void invalidateBuffer() {
    }

    @Override
    public long available() {
        return 0;
    }

    @Override
    public boolean read() {
        return false;
    }

    @Override
    public void close() {
    }
}
