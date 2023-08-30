/*
 * Copyright 2023 EPAM Systems, Inc
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