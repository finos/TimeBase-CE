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
package com.epam.deltix.qsrv.hf.tickdb.impl.disruptorqueue;

import com.epam.deltix.qsrv.hf.tickdb.impl.QuickMessageFilter;
import com.epam.deltix.qsrv.hf.tickdb.impl.bytedisruptor.ByteRingBuffer;

/**
 * @author Alexei Osipov
 */
final public class DisruptorQueueReaderLossless extends DisruptorQueueReader {
    DisruptorQueueReaderLossless(DisruptorMessageQueue disruptorMessageQueue, ByteRingBuffer ringBuffer, QuickMessageFilter filter, boolean polymorphic, boolean realTimeNotification) {
        super(disruptorMessageQueue, ringBuffer, filter, polymorphic, realTimeNotification);
    }

    @Override
    boolean pageDataIn() {
        boolean dataLoaded;
        if (isAsynchronous()) {
            dataLoaded = asyncPageIn();
        } else {
            dataLoaded = syncPageIn();
        }

        // We always have data in live mode
        assert dataLoaded || !isLive();

        // Publish all we have
        sequence.set(consumedSequence);
        return dataLoaded;
    }
}
