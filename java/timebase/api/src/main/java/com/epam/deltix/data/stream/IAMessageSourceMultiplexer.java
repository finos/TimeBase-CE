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
package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.TimeStampedMessage;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableCursor;
import com.epam.deltix.util.concurrent.NextResult;

import java.util.Comparator;

public class IAMessageSourceMultiplexer<T extends TimeStampedMessage> extends MessageSourceMultiplexer<T> implements IntermittentlyAvailableCursor {

    public IAMessageSourceMultiplexer() {
    }

    public IAMessageSourceMultiplexer(boolean ascending, boolean realTimeNotification, Comparator<T> c) {
        super(ascending, realTimeNotification, c);
    }

    public IAMessageSourceMultiplexer(boolean ascending, boolean realTimeNotification) {
        super(ascending, realTimeNotification);
    }

    @Override
    public NextResult           nextIfAvailable() {
        return syncNext(false);
    }

    @Override
    protected NextResult        moveNext(MessageSource<T> feed, boolean addEmpty) {
        if (feed instanceof IntermittentlyAvailableCursor) {
            NextResult result = ((IntermittentlyAvailableCursor) feed).nextIfAvailable();

            if (result == NextResult.UNAVAILABLE && addEmpty)
                addEmptySource(feed);

            return result;
        }

        return super.moveNext(feed, addEmpty);
    }
}