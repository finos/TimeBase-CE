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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;

import java.util.Enumeration;


// select sum{}(volume) from stream group by count(100)
public abstract class OverCountFilter extends FilterIMSImpl {

    private final int countInterval;
    private final boolean reset;
    private final boolean running;
    private Enumeration<FilterState> lastStates = null;

    public OverCountFilter(InstrumentMessageSource source, RecordClassDescriptor[] inputTypes,
                           RecordClassDescriptor[] outputTypes, ReadableValue[] params, DXTickDB db,
                           int countInterval, boolean reset, boolean running) {
        super(source, inputTypes, outputTypes, params, db);
        this.countInterval = countInterval;
        this.reset = reset;
        this.running = running;
    }

    @Override
    public boolean next() {
        if (hasPendingQueryStatus()) {
            return true;
        }

        boolean next = super.next();
        if (!next && lastStates == null && !running) {
            lastStates = getStates();
        }
        if (lastStates != null) {
            while (lastStates.hasMoreElements()) {
                FilterState state = lastStates.nextElement();
                if (state.messagesCount > 0) {
                    outMsg = state.getLastMessage();
                    switch (applyLimit(ACCEPT)) {
                        case ACCEPT:
                            return true;
                        case ABORT:
                            return false;
                    }
                }
            }
            return false;
        }
        return next;
    }

    @Override
    protected final int accept(RawMessage inMsg, FilterState state) {
        int status = acceptGroupByCount(inMsg, state);
        if (state == null) {
            return status;
        }

        if (status == ACCEPT)
            state.messagesCount++;
        if (state.messagesCount == countInterval) {
            state.messagesCount = 0;
            outMsg = state.getLastMessage();
            if (reset) {
                state.resetFunctions();
            }
            return ACCEPT;
        } else if (running) {
            return status;
        } else {
            return status == ACCEPT ? REJECT: status;
        }
    }

    protected void writeLast(FilterState state) {
        outMsg = state.getLastMessage();
    }

    protected abstract int acceptGroupByCount(RawMessage inMsg, FilterState state);
}