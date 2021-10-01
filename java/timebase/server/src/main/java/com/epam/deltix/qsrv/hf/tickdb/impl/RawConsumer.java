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

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataInput;

public class RawConsumer extends MessageConsumer<RawMessage> {

    private byte []                         bytes = new byte [128];
    private final RecordClassDescriptor[]   types;

    final TimeStamp time = new TimeStamp();

    public RawConsumer (RegistryCache registry, RecordClassDescriptor[] types, boolean realTimeNotification) {
        super(registry, types, realTimeNotification);
        this.types = types;
        this.message = new RawMessage();
    }

    @Override
    public void     process (int entity, long timestampNanos, int type, int bodyLength, MemoryDataInput mdi) {
        registry.decode(message, entity);
        message.setNanoTime(timestampNanos);
        message.type = types[type];
        currentTypeIndex = type;

        if (bytes.length < bodyLength)
            bytes = new byte [Util.doubleUntilAtLeast(bytes.length, bodyLength)];

        mdi.readFully(bytes, 0, bodyLength);

        message.offset = 0;
        message.data = bytes;
        message.length = bodyLength;
    }

    public RawMessage           getMessage() {
        return message;
    }

    @Override
    public RecordClassDescriptor getCurrentType() {
        if (currentTypeIndex == _REALTIME_MESSAGE_TYPE_INDEX)
            return Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR;
        return types[currentTypeIndex];
    }

    @Override
    protected RawMessage makeRealTimeStartMessage(long timestampNanos) {
        RawMessage message = new RawMessage(Messages.REAL_TIME_START_MESSAGE_DESCRIPTOR);
        message.setBytes(new byte[0], 0, 0);
        message.setSymbol("");
        message.setNanoTime(timestampNanos);
        return message;
    }

}