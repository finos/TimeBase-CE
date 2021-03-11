package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.generated.ObjectHashSet;

/**
 *
 */
final class LossyMessageQueue extends MessageQueue {
    private final ObjectHashSet <LossyMessageQueueReader>     inputStreamSet;

    LossyMessageQueue (TransientStreamImpl stream) {
        super (stream);

        inputStreamSet = new ObjectHashSet<>();
    }

    @Override
    boolean                         hasNoReaders () {
        synchronized (inputStreamSet) {
            return (inputStreamSet.isEmpty ());
        }
    }

    @Override
    void                            advanceBegin (MessageQueueReader s) {
    }

    @Override
    boolean                         advanceEnd (MessageQueueReader s) {
        return (false);
    }

    public void                     rawReaderClosed (LossyMessageQueueReader s) {
        synchronized (inputStreamSet) {
            inputStreamSet.remove (s);
        }

        synchronized (this) {
            waitingReaders.remove(s);
        }
    }

    @Override
    public LossyMessageQueueReader getRawReader() {
        LossyMessageQueueReader     reader =
                new LossyMessageQueueReader (this, stream.createEncoder(Messages.DATA_LOSS_MESSAGE_DESCRIPTOR));

        synchronized (inputStreamSet) {
            inputStreamSet.add (reader);
        }

        return (reader);
    }

    @Override
    public MessageChannel <InstrumentMessage>  getWriter (MessageEncoder <InstrumentMessage> encoder) {
        return (new LossyMessageQueueWriter (stream, this, encoder));
    }
}
