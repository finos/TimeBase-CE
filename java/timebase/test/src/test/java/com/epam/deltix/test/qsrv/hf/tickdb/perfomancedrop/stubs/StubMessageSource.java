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
package com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop.stubs;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.TypedMessageSource;
import com.epam.deltix.util.lang.DisposableListener;

import java.util.concurrent.TimeUnit;

/**
 * Special version of {@link MessageSource} for performance testing purposes.
 */
public class StubMessageSource<T extends InstrumentMessage> implements MessageSource<T>, TypedMessageSource {

    private DisposableListener listener;

    private final InstrumentMessage message;

    public StubMessageSource() {
        message = createRawMessage2();
    }

    @Override
    public T getMessage() {
        return (T) message;
    }

    @Override
    public boolean next() {
        return true;
    }

    @Override
    public boolean isAtEnd() {
        return false;
    }

    @Override
    public void close() {
        // disposed event notification
        if (listener != null)
            listener.disposed(this);
    }

    public void setAvailabilityListener(Runnable maybeAvailable) {

    }

    public long                 getStartTimestamp() {
        return System.currentTimeMillis();
    }

    public long                 getEndTimestamp() {
        return System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10);
    }


    public void                 setDisposableListener(DisposableListener<StubMessageSource> listener) {
        this.listener = listener;
    }

    private static RawMessage createRawMessage() {

        RecordClassDescriptor mmDescriptor = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        RecordClassDescriptor tradeDescriptor = StreamConfigurationHelper.mkTradeMessageDescriptor(
                mmDescriptor, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        RawMessage msg = new RawMessage();
        msg.type = tradeDescriptor;
        msg.data = new byte[10];
        msg.setSymbol("AAA");
        return msg;
    }

    private static InstrumentMessage createRawMessage2() {

        RecordClassDescriptor mmDescriptor = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        RecordClassDescriptor tradeDescriptor = StreamConfigurationHelper.mkTradeMessageDescriptor(
                mmDescriptor, null, null, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        RawMessage msg = new RawMessage();
        msg.type = tradeDescriptor;
        msg.data = new byte[128];
        msg.setSymbol("X");
        msg.offset = 0;
        msg.length = 50;
        msg.setNanoTime(System.nanoTime());
        return msg;
    }

    @Override
    public int getCurrentTypeIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RecordClassDescriptor getCurrentType() {
        throw new UnsupportedOperationException();
    }
}

