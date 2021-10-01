/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.util.*;

/**
 *  
 */
final class LosslessMessageQueue extends MessageQueue {
    /**
     *  The PQ is used to track open input streams when the
     *  no-loss option is on.
     */
    private final PriorityQueue <LosslessMessageQueueReader> inputStreamPQ;

    private final ArrayList<Runnable>       writerNotifiers =
        new ArrayList <Runnable> ();

    private volatile Runnable []            writerNotifierSnapshot = { };

    LosslessMessageQueue (TransientStreamImpl stream) {
        super (stream);
        
        inputStreamPQ = 
            new PriorityQueue <LosslessMessageQueueReader> (
                11,
                TDISComparator.INSTANCE
            );
    }

    private void                    updateNotifierSnapshot () {
        int     n = writerNotifiers.size ();

        writerNotifierSnapshot = writerNotifiers.toArray (new Runnable [n]);
    }

    void                     addWriterNotifier (Runnable notifier) {
        synchronized (writerNotifiers) {
            writerNotifiers.add (notifier);
            updateNotifierSnapshot ();
        }
    }

    void                     removeWriterNotifier (Runnable notifier) {
        synchronized (writerNotifiers) {
            writerNotifiers.remove (notifier);
            updateNotifierSnapshot ();
        }
    }

    @Override
    void                            notifyWaitingWriters () {
        assert !Thread.holdsLock (this);

        Runnable[] snapshot = writerNotifierSnapshot;
        for (int i = 0; i < snapshot.length; i++)
            snapshot[i].run();
    }

    @Override
    synchronized boolean            hasNoReaders () {
        return (inputStreamPQ.isEmpty ());
    }

    @Override
    void                            advanceBegin (MessageQueueReader s) {
        assert Thread.holdsLock (this);

        inputStreamPQ.remove ((LosslessMessageQueueReader) s);
    }

    @Override
    boolean                         advanceEnd (MessageQueueReader s) {
        assert Thread.holdsLock (this);

        inputStreamPQ.offer ((LosslessMessageQueueReader) s);

        return (rollQueue ());
    }

    public void                     rawReaderClosed (LosslessMessageQueueReader lmqr) {

        boolean                         queueWasRolled;

        synchronized (this) {
            waitingReaders.remove(lmqr);

            inputStreamPQ.remove (lmqr);
            queueWasRolled = rollQueue ();
        }

        if (queueWasRolled)
            notifyWaitingWriters ();
    }

    private boolean                 rollQueue () {
        assert Thread.holdsLock (this);

        LosslessMessageQueueReader   head = inputStreamPQ.peek ();

        return (
            head == null ?
                clearBuffer () :
                discardHead (head.getCurrentOffset())
        );
    }

    @Override
    public synchronized LosslessMessageQueueReader getRawReader () {
        LosslessMessageQueueReader  s = new LosslessMessageQueueReader (this);
        inputStreamPQ.offer (s);
        
        return (s);
    }

    @Override
    public MessageChannel<InstrumentMessage> getWriter(
        MessageEncoder<InstrumentMessage> encoder
    )
    {
        return (new LosslessMessageQueueWriter (stream, this, encoder));
    }
}