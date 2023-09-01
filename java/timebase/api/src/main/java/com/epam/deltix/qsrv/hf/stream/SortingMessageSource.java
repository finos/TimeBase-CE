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
package com.epam.deltix.qsrv.hf.stream;

import java.util.PriorityQueue;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.MessageTimeComparator;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

/**
 * This class reorders messages coming from nested message source (in time order).
 *
 * This class should only be used in feed simulation mode - it introduces a latency
 * when certain amount of input messages are accumulated to ensure proper order.
 *
 * FIXME: This class has severe problem: it doesn't copy messages when placing them in PQ.
 * Thus it doesn't work with message source that reuses messages.
 *
 */
public class SortingMessageSource<T extends TimeStampedMessage> implements MessageSource<T> {
    private final MessageSource<T> delegate;
    private final long maxBufferDuration;
    private final PriorityQueue<T> buffer = new PriorityQueue<T>(256, MessageTimeComparator.INSTANCE);

    private boolean isDelegateAtEnd = false;
    private long oldestMessageTime = Long.MIN_VALUE;
    private T currentMessage;

    /**
     * @param maxBufferDuration specifies maximum anticipated delay of 'late' messages [in milliseconds].
     * For example, for RedSky feed messages may be delayed for up to 3 seconds.
     */
    public SortingMessageSource(MessageSource<T> delegate, long maxBufferDuration) {
        this.delegate = delegate;
        this.maxBufferDuration = maxBufferDuration;
    }

    @Override
    public boolean next() {
        currentMessage = null;

        if ( ! isDelegateAtEnd) {
            while (getBufferDuration () < maxBufferDuration) {
                if (delegate.next()) {
                    T message = delegate.getMessage();
                    oldestMessageTime = Math.max(oldestMessageTime, message.getTimeStampMs());
                    buffer.add(message);
                } else {
                    isDelegateAtEnd = true;
                    break;
                }
            }
        }

        if (buffer.isEmpty())
            return false;

        currentMessage = buffer.remove();
        return true;
    }

    private long getBufferDuration () {
        if (buffer.isEmpty())
            return 0L;
        return oldestMessageTime - buffer.peek().getTimeStampMs();
    }

    @Override
    public T getMessage() {
        return currentMessage;
    }

    @Override
    public boolean isAtEnd() {
        return currentMessage == null && buffer.isEmpty() && isDelegateAtEnd;
    }


    @Override
    public void close() {
        buffer.clear();
        delegate.close();
    }

}