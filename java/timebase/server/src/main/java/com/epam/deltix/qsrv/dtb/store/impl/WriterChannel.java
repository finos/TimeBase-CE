package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.dtb.store.pub.DataWriter;
import com.epam.deltix.qsrv.dtb.store.pub.IllegalMessageAppend;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.MessageProducer;
import com.epam.deltix.qsrv.hf.tickdb.impl.RegistryCache;
import com.epam.deltix.qsrv.hf.tickdb.pub.OutOfSequenceMessageException;
import com.epam.deltix.qsrv.hf.tickdb.pub.WriterClosedException;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.TimeKeeper;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class WriterChannel implements MessageChannel<InstrumentMessage> {

    private DataWriter writer;

    private final MessageProducer       producer;
    private final RegistryCache         cache;
    private boolean                     opened = false;
    private final MutableBoolean        exists = new MutableBoolean(false);

    public WriterChannel(DataWriter writer,
                    MessageProducer<? extends InstrumentMessage> producer,
                    RegistryCache cache)
    {
        this.writer = writer;
        this.producer = producer;
        this.cache = cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized void send(InstrumentMessage msg) {
        if (writer == null) // closed state
            throw new WriterClosedException(this + " is closed");

        boolean undefined = msg.getTimeStampMs() == TimeStampedMessage.TIMESTAMP_UNKNOWN;

        long nstime = undefined ? TimeKeeper.currentTimeNanos : msg.getNanoTime();

        assert nstime != Long.MAX_VALUE; // temporary

        if (!opened) {
            writer.open(nstime, null);
            opened = true;
        }

        int index = cache.encode(msg, exists);
        int type = producer.beginWrite(msg);

        try {
            writer.appendMessage(index, nstime, type, producer, false);
        } catch (IllegalMessageAppend e) {
            throw new OutOfSequenceMessageException(msg, nstime, "", e.getLastWrittenNanos());
        }
    }

    @Override
    public synchronized void close() {
        Util.close(writer);
        writer = null;
        opened = false;
    }
}
