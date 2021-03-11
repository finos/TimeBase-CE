package com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.ByteRingBuffer;
import com.epam.deltix.qsrv.hf.tickdb.pub.WriterAbortedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.WriterClosedException;

/**
 * @author Alexei Osipov
 */
abstract class DisruptorQueueWriter implements MessageChannel<InstrumentMessage> {
    protected final DisruptorMessageQueue queue;
    protected final ByteRingBuffer ringBuffer;
    protected final InstrumentMessageInteractiveEventWriter writer;

    private volatile boolean isOpen = true;

    DisruptorQueueWriter(DisruptorMessageQueue queue, ByteRingBuffer ringBuffer, MessageEncoder<InstrumentMessage> encoder) {
        this.queue = queue;
        this.ringBuffer = ringBuffer;
        this.writer = new InstrumentMessageInteractiveEventWriter(encoder);
    }

    @Override
    public void send(InstrumentMessage msg) {
        if (!isOpen) {
            throw new WriterClosedException(this + " is closed");
        }
        if (queue.closed) {
            throw new WriterAbortedException(queue.stream + " is closed");
        }

        if (!queue.stream.accumulateIfRequired(msg)) {
            return;
        }

        int length = writer.prepare(msg);
        writeDataToRingBuffer(length);
    }

    protected abstract void writeDataToRingBuffer(int length);

    @Override
    public void close() {
        isOpen = false;
    }

}
