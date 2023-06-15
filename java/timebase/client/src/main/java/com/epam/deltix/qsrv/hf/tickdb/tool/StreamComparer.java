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
package com.epam.deltix.qsrv.hf.tickdb.tool;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.replication.StreamStorage;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.collections.ReusableObjectPool;
import com.epam.deltix.util.lang.Factory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StreamComparer {
    private static final Log LOG = LogFactory.getLog(StreamComparer.class);
    private static final Comparator<RawMessage> DEFAULT_RAW_COMPARATOR = new RawMessageComparator(true, true, false);

    public enum ComparerType {
        Ordered, Unordered
    }

    protected final Comparator<RawMessage> comparator;

    public StreamComparer(Comparator<RawMessage> comparator) {
        this.comparator = comparator;
    }

    public StreamComparer() {
        this(DEFAULT_RAW_COMPARATOR);
    }

    public static StreamComparer create(ComparerType type) {
        Comparator<RawMessage> comparator = DEFAULT_RAW_COMPARATOR;
        return type == ComparerType.Ordered ? new StreamComparer(comparator) : new UnorderedStreamComparer(comparator);
    }

    public boolean compare(StreamStorage first, StreamStorage second) {
        return compare(first.getSource(), second.getSource(), null);
    }

    public boolean compare(DXTickStream first, DXTickStream second, IdentityKey[] entities) {
        String firstName = first.getKey().equals(second.getKey()) ? "(FST)." + first.getKey() : first.getKey();
        String secondName = first.getKey().equals(second.getKey()) ? "(SND)." + second.getKey() : second.getKey();

        LOG.info("Compare streams: [%s] and [%s]").with(firstName).with(secondName);

        SelectionOptions cursorOptions = new SelectionOptions(true, false);
        try (TickCursor firstCursor = first.select(Long.MIN_VALUE, cursorOptions, null, entities)) {
            try (TickCursor secondCursor = second.select(Long.MIN_VALUE, cursorOptions, null, entities)) {
                TimeStamp prevTimestamp = new TimeStamp(); // Undefined

                int counter = 0;
                boolean hasFirst = firstCursor.next();
                boolean hasSecond = secondCursor.next();
                while (hasFirst && hasSecond) {
                    counter++;

                    RawMessage firstMessage = (RawMessage) firstCursor.getMessage();
                    RawMessage secondMessage = (RawMessage) secondCursor.getMessage();

                    if (LOG.isTraceEnabled()) {
                        LOG.trace("#%s (%s): %s").with(counter).with(firstName).with(firstMessage);
                        LOG.trace("#%s (%s): %s").with(counter).with(secondName).with(secondMessage);
                    }

                    if (firstMessage.getTimeStampMs() < prevTimestamp.timestamp) {
                        LOG.error("Message #%s: timestamps goes back: \n\t%s\n\t%s").with(counter).with(firstMessage).with(prevTimestamp.getTime());
                    }

                    // compare message timestamps
                    if (firstMessage.compareTime(secondMessage) != 0) {
                        LOG.error("Message #%s: Messages timestamps are different: \n\t%s\n\t%s").with(counter).with(firstMessage).with(secondMessage);
                        return false;
                    }

                    if (!compareMessages(counter, prevTimestamp, firstMessage, secondMessage))
                        return false;

                    hasFirst = firstCursor.next();
                    hasSecond = secondCursor.next();
                }
                LOG.info("Stop streams comparison. Processed message count: %s").with(counter);

                if (!firstCursor.isAtEnd() && secondCursor.isAtEnd()) {
                    // make sure that we not have data
                    if (firstCursor.next()) {
                        LOG.error("Stream [%s] has more messages than stream [%s]").with(firstName).with(secondName);
                        return false;
                    }
                }
                else if (firstCursor.isAtEnd() && !secondCursor.isAtEnd()) {
                    // make sure that we not have data
                    if (secondCursor.next()) {
                        LOG.error("Stream [%s] has more messages than stream [%s]").with(firstName).with(secondName);
                        return false;
                    }
                }
            }
        }
        LOG.warn("Streams [%s] and [%s] are identical").with(firstName).with(secondName);
        return true;
    }

    protected boolean compareMessages(long counter, TimeStamp prevTimestamp, RawMessage firstMessage, RawMessage secondMessage) {
        if (comparator.compare(firstMessage, secondMessage) != 0) {
            LOG.error("Message #%s: Messages are different: \n\t%s\n\t%s").with(counter).with(firstMessage).with(secondMessage);
            return false;
        }
        prevTimestamp.setNanoTime(firstMessage.getNanoTime());
        return true;
    }

    /////////////////////////// HELPER CLASSES /////////////////////////////

    private static final class UnorderedStreamComparer extends StreamComparer {
        private static final int RAW_BUFFER_SIZE = 512;

        private final List<RawMessage> firstBuffer = new ArrayList<>(16);
        private final List<RawMessage> secondBuffer = new ArrayList<>(16);

        private final ReusableObjectPool<RawMessage> messagePool = ReusableObjectPool.create(new Factory<RawMessage>() {
            @Override
            public RawMessage create() {
                RawMessage message = new RawMessage();
                message.setBytes(new byte[RAW_BUFFER_SIZE], 0, 0);
                return message;
            }
        }, 16);

        public UnorderedStreamComparer(Comparator<RawMessage> comparator) {
            super(comparator);
        }

        private void clearBuffers() {
            for (int i = 0; i < firstBuffer.size(); i++)
                messagePool.release(firstBuffer.get(i));
            firstBuffer.clear();

            for (int i = 0; i < secondBuffer.size(); i++)
                messagePool.release(secondBuffer.get(i));
            secondBuffer.clear();
        }

        private void addToBuffers(RawMessage first, RawMessage second) {
            RawMessage fst = messagePool.borrow();
            fst.copyFrom(first);
            firstBuffer.add(fst);

            RawMessage scd = messagePool.borrow();
            scd.copyFrom(second);
            secondBuffer.add(scd);
        }

        private boolean compareBuffers() {
            if (firstBuffer.size() != secondBuffer.size())
                return false;
            if (firstBuffer.isEmpty() && secondBuffer.isEmpty())
                return true;

            for (int i = 0; i < firstBuffer.size(); i++) {
                RawMessage firstMsg = firstBuffer.get(i);
                boolean found = false;
                for (int j = 0; j < secondBuffer.size(); j++) {
                    RawMessage secondMsg = secondBuffer.get(j);
                    if (comparator.compare(firstMsg, secondMsg) == 0) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return false;
            }

            return true;
        }

        @Override
        protected boolean compareMessages(long counter, TimeStamp prevTimestamp, RawMessage firstMessage, RawMessage secondMessage) {
            // check transition to next time
            if (prevTimestamp.isUndefined() || firstMessage.compareTime(prevTimestamp) != 0) {
                // compare messages in buffer for prev time
                if (!prevTimestamp.isUndefined() && !compareBuffers()) {
                    LOG.error("Messages for %s time are different").withTimestamp(prevTimestamp.getTimeStampMs());
                    return false;
                }

                if (comparator.compare(firstMessage, secondMessage) != 0) {
                    // clear 'same time' message buffers
                    clearBuffers();

                    // remember current messages
                    addToBuffers(firstMessage, secondMessage);

                    // change prev time
                    prevTimestamp.setNanoTime(firstMessage.getNanoTime());
                } else {
                    // set undefined as flag of successful comparison
                    prevTimestamp.setUndefined();
                }

            } else {
                // remember messages with the same time
                addToBuffers(firstMessage, secondMessage);
            }
            return true;
        }
    }
}