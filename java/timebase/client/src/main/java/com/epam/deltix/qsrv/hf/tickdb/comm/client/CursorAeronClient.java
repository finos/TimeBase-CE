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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.util.lang.Disposable;
import io.aeron.*;
import io.aeron.logbuffer.ControlledFragmentHandler;

/**
 * @author Alexei Osipov
 */
public class CursorAeronClient implements Disposable {
    public static final String CHANNEL = TDBProtocol.AERON_CHANNEL;
    public static final int MAX_MESSAGES_TO_GET = 1000;

    private final Subscription dataSubscription;
    private final ControlledFragmentAssembler fragmentHandler;

    private CursorAeronClient(Subscription dataSubscription, ControlledFragmentHandler delegate) {
        this.dataSubscription = dataSubscription;
        this.fragmentHandler = new ControlledFragmentAssembler(delegate);
    }

    public static CursorAeronClient create(int aeronDataStreamId, ControlledFragmentHandler delegate, Aeron aeron, String aeronChannel) {
        Subscription dataSubscription = aeron.addSubscription(aeronChannel, aeronDataStreamId);
        return new CursorAeronClient(dataSubscription, delegate);
    }

    public boolean pageDataIn() {
        return dataSubscription.controlledPoll(fragmentHandler, MAX_MESSAGES_TO_GET) > 0;
    }

    public boolean isClosed() {
        return false;
    }

    @Override
    public void close() {
        dataSubscription.close();
    }

    public Subscription getSubscription() {
        return dataSubscription;
    }
}