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
package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.data.stream.RealTimeMessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableResource;
import com.epam.deltix.util.concurrent.UnavailableResourceException;

/**
 * Always throws UnavailableResourceException on next();
 */
public class PermanentlyUnavailableMessageSource implements RealTimeMessageSource<InstrumentMessage>, IntermittentlyAvailableResource {

    @Override
    public void setAvailabilityListener(Runnable listener) {
        // we will never call back
    }

    @Override
    public InstrumentMessage getMessage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean next() {
        throw UnavailableResourceException.INSTANCE;
    }

    @Override
    public boolean isAtEnd() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isRealTime() {
        return true;
    }

    @Override
    public boolean realTimeAvailable() {
        return true;
    }
}
