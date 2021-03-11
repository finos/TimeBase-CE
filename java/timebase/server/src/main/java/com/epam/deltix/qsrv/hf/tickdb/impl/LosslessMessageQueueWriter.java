package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.data.stream.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;


/**
 *
 */
class LosslessMessageQueueWriter extends MessageQueueWriter <LosslessMessageQueue> {
    private final Runnable                                  writerNotifier =
        new Runnable () {
            public void run () {
                notifySpaceAvailable ();
            }
        };

    private boolean                                         isWaiting = false;
    
    LosslessMessageQueueWriter (
        TransientStreamImpl                         stream,
        LosslessMessageQueue                        queue,
        MessageEncoder <InstrumentMessage>          encoder
    )
    {
        super (stream, queue, encoder);

        queue.addWriterNotifier (writerNotifier);
    }

    private synchronized void       notifySpaceAvailable () {
        if (isWaiting)
            notify ();
    }

    public synchronized void        send (InstrumentMessage msg) {
        if (!prepare (msg))
            return;

        // timestamp may change in writeBuffer
        long time = msg.getTimeStampMs();

//        int     spinCount = 0;
        for (;;) {
            try {
                writeBuffer(time);
                break;
            } catch (UnavailableResourceException x) {
//                spinCount++;
//
//                if (spinCount < 30)
//                    continue;
//
//                spinCount = 0;
                isWaiting = true;
                try {
                    wait ();
                } catch (InterruptedException ix) {
                    throw new UncheckedInterruptedException (ix);
                } finally {
                    isWaiting = false;
                }
            }
        }
    }

    public void                     close () {
        super.close();
        queue.removeWriterNotifier (writerNotifier);        
    }
}
