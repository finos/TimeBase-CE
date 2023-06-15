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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.time.GMT;

/**
 * Notifies a client that an out-of-sequence message cannot be stored to Timebase.
 */
public class OutOfSequenceMessageException extends LoadingMessageException {
    private String stream;
    private final long lastWrittenNanos;

    public OutOfSequenceMessageException(InstrumentMessage msg, String stream, long lastWrittenNanos) {
        super(msg);
        this.stream = stream;
        this.lastWrittenNanos = lastWrittenNanos;
    }

    public OutOfSequenceMessageException(InstrumentMessage msg, long nstime, String stream, long lastWrittenNanos) {
        super(msg);
        this.nanoTime = nstime;
        this.stream = stream;
        this.lastWrittenNanos = lastWrittenNanos;
    }

    @Override
    public String getMessage() {
        return String.format("[%s] Message %s %s GMT is out of sequence (< %s)",
                stream, symbol, GMT.formatNanos(nanoTime), GMT.formatNanos(lastWrittenNanos));
    }
}