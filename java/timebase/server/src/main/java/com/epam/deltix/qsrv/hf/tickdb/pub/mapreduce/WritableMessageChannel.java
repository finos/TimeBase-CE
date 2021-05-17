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
package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import org.apache.hadoop.io.LongWritable;

import java.io.IOException;

/**
 *
 */
public class WritableMessageChannel<T extends WritableMessage> implements MessageChannel<InstrumentMessage> {
    private final MessageMapper<LongWritable, T>.Context context;
    private final LongWritable time = new LongWritable(0);
    private final T writable;

    public WritableMessageChannel(final T writable, final MessageMapper<LongWritable, T>.Context context) {
        this.writable = writable;
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void send(InstrumentMessage msg) {
        try {
            time.set(msg.getNanoTime());
            writable.set(msg, context);
            context.write(time, writable);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
    }
}
