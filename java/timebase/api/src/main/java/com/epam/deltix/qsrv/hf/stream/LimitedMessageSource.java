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

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 *  MessageSource that simulates end-of-stream at given timestamp
*/
public class LimitedMessageSource implements MessageSource<InstrumentMessage> {

    private MessageSource<InstrumentMessage> delegate;
    private final long limit;
    private boolean keepReading = true;

    public LimitedMessageSource(MessageSource<InstrumentMessage> delegate, long limit) {
        this.delegate = delegate;
        this.limit = limit;
    }

    @Override
    public InstrumentMessage getMessage() {
        assert keepReading;

        return delegate.getMessage();
    }

    @Override
    public boolean isAtEnd() {
        return ! keepReading;
    }

    @Override
    public boolean next() {
        if (keepReading) {
            keepReading = delegate.next();
            if (keepReading) {
                InstrumentMessage msg = delegate.getMessage();
                keepReading = (msg.getTimeStampMs() < limit);
            }
        }
        return keepReading;
    }

    @Override
    public void close() {
        delegate.close();
    }
}