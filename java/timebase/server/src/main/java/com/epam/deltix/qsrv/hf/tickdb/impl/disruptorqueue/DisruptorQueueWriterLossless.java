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
package com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue;

import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.ByteRingBuffer;

/**
 * Writer for lossless queue. Before using read notes below.
 * <p>
 * <p><b>WARNING</b>: This writer will do busy spin with Thread.yield() in case of full queue.
 * <b>Thread will NOT react to {@link Thread#interrupt()}.
 * Thread will NOT react to {@link #close()}.
 * It will spin till it writes message to the queue.</b>
 * </p>
 *
 * @author Alexei Osipov
 */
final class DisruptorQueueWriterLossless extends DisruptorQueueWriter {
    DisruptorQueueWriterLossless(DisruptorMessageQueue queue, ByteRingBuffer ringBuffer, MessageEncoder<InstrumentMessage> encoder) {
        super(queue, ringBuffer, encoder);
    }

    @Override
    protected void writeDataToRingBuffer(int length) {
        // We have to synchronize because we use Disruptor in SingleProducer mode
        synchronized (queue.writeLock) {
            // WARNING: This #next call may loop lndifinitely on Thread.yield()
            long hi = ringBuffer.next(length); // Inclusive
            ringBuffer.writeToAllocatedRange(writer, hi, length);
        }
    }
}