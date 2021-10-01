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

/**
 * A superclass for all exceptions, which might occur as a result of message loading ({@link com.epam.deltix.data.stream.MessageChannel#send MessageChannel.send})
 */
public class LoadingMessageException extends LoadingError {
    protected final String symbol;
    protected long nanoTime;

    public LoadingMessageException(InstrumentMessage msg) {
        symbol = msg.getSymbol().toString();
        nanoTime = msg.getNanoTime();
    }
}
