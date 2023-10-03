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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.computations.api.*;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.annotations.TimestampNs;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.util.Enumeration;

public abstract class OverTimeFilter extends FilterIMSImpl {

    private final TimeSaver timeSaver;
    private final long timeInterval;
    protected final boolean reset;
    protected final boolean trigger;
    protected final boolean every;
    protected final boolean running;
    private boolean initialized = false;
    private long emptyMessages = 0;
    private boolean writingEmptyMessages = false;
    @TimestampNs
    private long nanoTime = -1;
    private Enumeration<FilterState> lastStates = null;
    private Enumeration<FilterState> intervalStates = null;

    public OverTimeFilter(InstrumentMessageSource source, RecordClassDescriptor[] inputTypes,
                          RecordClassDescriptor[] outputTypes, ReadableValue[] params, DXTickDB db,
                          long timeInterval, boolean reset, boolean trigger, boolean every, boolean running,
                          boolean forward) {
        super(source, inputTypes, outputTypes, params, db);
        this.timeInterval = timeInterval;
        this.reset = reset;
        this.trigger = trigger;
        this.every = every;
        this.running = running;
        this.timeSaver = forward ? PrettyTimeSaver.create() : ReversePrettyTimeSaver.create();
    }

    @Override
    public boolean next() {
        if (nextStatusMessage()) {
            return true;
        }

        // processing current interval states
        while (nextIntervalState()) {
            switch (applyLimit(ACCEPT)) {
                case ABORT:     return false;
                case ACCEPT:    return true;
            }
        }
        // processing empty messages
        while (nextEmptyState()) {
            switch (applyLimit(ACCEPT)) {
                case ABORT:     return false;
                case ACCEPT:    return true;
            }
        }
        if (lastStates != null) {
            while (lastStates.hasMoreElements()) {
                FilterState state = lastStates.nextElement();
                if (!state.havingAccepted) {
                    continue;
                }
                writeLast(state);
                if (outMsg.data != null) {
                    lastState = state;
                    switch (applyLimit(ACCEPT)) {
                        case ABORT:     return false;
                        case ACCEPT:    return true;
                    }
                }
            }
            return false;
        }
        boolean next = super.next();
        if (!next && lastStates == null) {
            lastStates = getStates();
        }
        //
        if (aggregatedMessages > 0) {
            while (lastStates != null && nextLast()) {
                switch (applyLimit(ACCEPT)) {
                    case ABORT:
                        return false;
                    case ACCEPT:
                        return true;
                }
            }
            return next || nextLast();
        }

        return next;
    }

    protected final int acceptFirst(RawMessage inMsg, FilterState state) {
        timeSaver.reset(inMsg.getNanoTime(), timeInterval);
        emptyMessages = timeSaver.put(inMsg.getNanoTime()); // couldn't be > -1 on this iteration, cause it's first value
        int result = acceptGroupByTime(inMsg, state);
        if (result == REJECT || result == ABORT) {
            return result;
        } else {
            initialized = true;
            state.initializedOnInterval = true;
            state.initialized = true;
            aggregatedMessages++;
            return running ? result: REJECT;
        }
    }

    @Override
    protected final int accept(RawMessage inMsg, FilterState state) {
        if (state != null) {
            if (!initialized) {
                return acceptFirst(inMsg, state);
            }
            if (nextIntervalState()) {
                state.waitingByTime = true;
                return ACCEPT;
            }
            if (nextEmptyState()) {
                state.waitingByTime = true;
                return ACCEPT;
            }
            if (state.waitingByTime) {
                state.waitingByTime = false;
            }
            emptyMessages = timeSaver.put(inMsg.getNanoTime());
            if (emptyMessages >= 0) {
                intervalStates = getStates();
                if (running && reset) {
                    while (intervalStates.hasMoreElements()) {
                        intervalStates.nextElement().resetFunctions();
                    }
                    intervalStates = null;
                } else if (nextIntervalState()) {
                    state.waitingByTime = true;
                    return ACCEPT;
                }
            }
        }
        int result = acceptGroupByTime(inMsg, state);
        if (state == null) {
            return result;
        }

        if (result == ACCEPT && !state.initializedOnInterval) {
            state.initializedOnInterval = true;
            if (!state.initialized) {
                state.initialized = true;
            }
        }
        if (result == ACCEPT) {
            aggregatedMessages++;
        }

        if (result == ABORT) {
            return ABORT;
        } else if (running) {
            return result;
        } else {
            return REJECT;
        }
    }

    private boolean nextIntervalState() {
        if (intervalStates == null || writingEmptyMessages) {
            return false;
        }
        while (intervalStates.hasMoreElements()) {
            FilterState state = intervalStates.nextElement();
            if (!state.initializedOnInterval && !every || !state.initialized)
                continue;
            outMsg = state.getLastMessage();
            outMsg.setNanoTime(timeSaver.getReady());
            if (outMsg.data == null || (!state.initializedOnInterval && !trigger)) {
                state.getOut().reset();
                encodeNull(state.getOut());
                outMsg.setBytes(state.getOut());
            }
            state.waitingByTime = true;
            if (!trigger || reset) {
                state.resetFunctions();
            }
            state.initializedOnInterval = false;
            lastState = state;
            if (!state.havingAccepted) {
                continue;
            }
            return true;
        }
        intervalStates = null;
        return false;
    }

    private boolean nextEmptyState() {
        if (!every || emptyMessages < 1) {
            return false;
        }
        if (!writingEmptyMessages) {
            writingEmptyMessages = true;
        }
        if (intervalStates == null) {
            intervalStates = getStates();
        }
        if (nanoTime == -1) {
            nanoTime = timeSaver.getReady() + timeSaver.getStep();
        }
        while (emptyMessages > 0) {
            if (intervalStates.hasMoreElements()) {
                FilterState state = intervalStates.nextElement();
                if (!state.initialized)
                    continue;
                if (!trigger) {
                    state.getOut().reset();
                    encodeNull(state.getOut());
                    state.getLastMessage().setBytes(state.getOut());
                }
                outMsg.setNanoTime(nanoTime);
                outMsg = state.getLastMessage();
                lastState = state;
                if (!state.havingAccepted) {
                    continue;
                }
                return true;
            }
            emptyMessages--;
            intervalStates = getStates();
            nanoTime += timeSaver.getStep();
        }
        intervalStates = null;
        nanoTime = -1;
        writingEmptyMessages = false;
        return false;
    }

    protected boolean nextLast() {
        if (lastStates == null) {
            return false;
        }
        while (lastStates.hasMoreElements()) {
            FilterState state = lastStates.nextElement();
            if (!state.havingAccepted) {
                continue;
            }
            writeLast(state);
            if (outMsg.data != null) {
                lastState = state;
                return true;
            }
        }
        lastStates = null;
        return false;
    }

    protected void writeLast(FilterState state) {
        outMsg = state.getLastMessage();
        outMsg.setNanoTime(timeSaver.getEnd());
    }

    protected abstract void encodeNull(MemoryDataOutput mdo);

    protected abstract int acceptGroupByTime(RawMessage inMsg, FilterState state);
}