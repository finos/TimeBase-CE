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

import com.epam.deltix.qsrv.hf.blocks.InstrumentIndex;
import com.epam.deltix.qsrv.hf.codec.cg.StringBuilderPool;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.ClassSet;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.messages.QueryStatus;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.InstancePool;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.util.collections.IndexedArrayList;
import java.util.Arrays;
import java.util.Enumeration;

/**
 *
 */
public abstract class FilterIMSImpl
        extends DelegatingInstrumentMessageSource
        implements FixedMessageSource
{
    protected static final int                  REJECT = 0;
    protected static final int                  ACCEPT = 1;
    protected static final int                  ABORT = -1;

    protected boolean                           atEnd = false;
    protected final RecordClassDescriptor []    inputTypes;
    protected final RecordClassDescriptor []    outputTypes;
    protected final ClassSet<? extends ClassDescriptor> outputClassSet;
    protected final ReadableValue []            params;
    protected final DXTickDB db;
    private final int []                        inputTypeIndexMap;
    private final InstrumentIndex               instrumentIndex =
            new InstrumentIndex ();

    private final IndexedArrayList <String>     streamKeyIndex =
            new IndexedArrayList <String> ();

    private final IndexedArrayList <RecordClassDescriptor> typeIndex =
            new IndexedArrayList <RecordClassDescriptor> ();

    protected RawMessage                        outMsg;
    private Enumeration <FilterState>           aggregateEnum = null;

    protected FilterStateProvider stateProvider;

    protected final StringBuilderPool varcharPool = new StringBuilderPool();
    protected final InstancePool instancePool = new InstancePool();

    private FilterState lastState;
    private int waitingMessagesCount;
    private int currentWaitingMessage;
    private long offset = Long.MIN_VALUE;
    private long limit = Long.MIN_VALUE;

    private final QueryStatusMessageProvider queryStatusMessageProvider;
    private RawMessage queryStatusMessage;
    private boolean groupByWarningSent;
    private long firstMessageTimestamp = Long.MIN_VALUE;

    protected FilterIMSImpl (
            InstrumentMessageSource             source,
            RecordClassDescriptor []            inputTypes,
            RecordClassDescriptor []            outputTypes,
            ReadableValue []                    params,
            DXTickDB db
    )
    {
        super (source);

        this.outputTypes = outputTypes;
        this.inputTypes = inputTypes;
        this.params = params;
        this.outputClassSet = new RecordClassSet(outputTypes);
        this.db = db;

        this.inputTypeIndexMap = new int [inputTypes.length];
        Arrays.fill (inputTypeIndexMap, -1);
        stateProvider = newStateProvider(this);

        queryStatusMessageProvider = new QueryStatusMessageProvider(outputTypes);
    }

    protected abstract FilterStateProvider newStateProvider(FilterIMSImpl filter);
    public abstract FilterState getInterimState();

    protected FilterState getState(RawMessage msg) {
        return stateProvider.getState(msg);
    }

    protected Enumeration<FilterState> getStates() {
        return stateProvider.getStates();
    }

    protected void restartAggregation() {
        stateProvider.startAggregate();
    }

    protected void stopAggregation() {
        stateProvider.stopAggregate();
    }

    private boolean hasUnprocessedGroups() {
        return stateProvider.hasUnprocessedGroups();
    }

    public int acceptInterimState(RawMessage msg) {
        return accept(msg, null);
    }

    protected void clearPools() {
        this.varcharPool.reset();
        this.instancePool.reset();
    }

    InstrumentMessageSource getSource() {
        return source;
    }

    @Override
    public int                              getCurrentStreamIndex () {
        if (source.getCurrentStream() != null)
            return (streamKeyIndex.getIndexOrAdd (getCurrentStreamKey ()));

        return -1;
    }

    @Override
    public int                              getCurrentEntityIndex () {
        return (instrumentIndex.getOrAdd (getMessage ()));
    }

    @Override
    public RawMessage                       getMessage () {
        return (outMsg);
    }

    @Override
    public RecordClassDescriptor            getCurrentType () {
        return (getMessage ().type);
    }

    @Override
    public int                              getCurrentTypeIndex () {
        return (typeIndex.getIndexOrAdd (getCurrentType ()));
    }

    /**
     *  Efficiently returns an index of the current message type in
     *  the array of output types returned by {@link #getMessageTypes}.
     */
    protected int                          getInputTypeIndex () {
        int     ctix = source.getCurrentTypeIndex ();
        int     out = inputTypeIndexMap [ctix];

        if (out == -1) {
            RecordClassDescriptor   curType = source.getCurrentType ();

            for (int ii = 0; ii < inputTypes.length; ii++) {
                if (curType.equals (inputTypes [ii])) {
                    out = ii;
                    break;
                }
            }

            if (out == -1)
                throw new IllegalStateException (
                        "Type " + curType +
                                " is not found in the preset list of input types"
                );

            inputTypeIndexMap [ctix] = out;
        }

        return (out);
    }

    @Override
    public boolean                      isAtEnd () {
        return (atEnd);
    }

    protected int                       accept (RawMessage inMsg, FilterState state) {
        outMsg = inMsg;
        return (ACCEPT);
    }

    /**
     *  Aggregate queries override next() to call this method.
     */
    protected boolean                   nextAggregated () {
        try {
            if (aggregateEnum == null) {
                for (;;) {
                    if (hasPendingQueryStatus()) {
                        return true;
                    }

                    if (!hasWaitingMessages()) {
                        if (!source.next()) {
                            break;
                        }
                    }

                    saveFirstTimestamp();

                    int state = getStateAndProcess();
                    if (state == ABORT)
                        break;
                }

                aggregateEnum = getStates();
            }

            for (;;) {
                if (!aggregateEnum.hasMoreElements()) {
                    if (hasUnprocessedGroups()) {
                        aggregateEnum = null;
                        source.reset(firstMessageTimestamp);
                        restartAggregation();
                        return nextAggregated();
                    } else {
                        stopAggregation();
                        return (false);
                    }
                }

                FilterState state = aggregateEnum.nextElement();

                if (state.accepted) {
                    int status = applyLimit(ACCEPT);
                    if (status == REJECT) {
                        continue;
                    } else if (status == ABORT) {
                        return false;
                    } else {
                        outMsg = state.getLastMessage();
                        return (true);
                    }
                }
            }
        } catch (Throwable t) {
            stopAggregation();
            throw t;
        }
    }

    @Override
    public boolean                      next () {
        if (hasPendingQueryStatus()) {
            return true;
        }

        for (;;) {
            if ((lastState == null || !lastState.waitingByTime) && !hasWaitingMessages()) {
                if (!source.next()) {
                    atEnd = true;
                    return (false);
                }
            }

            int s = getStateAndProcess();
            s = applyLimit(s);

            switch (s) {
                case ABORT:     return (false);
                case ACCEPT:    return (true);
            }
        }
    }

    protected int getStateAndProcess() {
        final RawMessage inMsg = (RawMessage) source.getMessage();
        final FilterState state = lastState = getState(inMsg);
        prepareGroupByWarning(state);
        final int status = accept(inMsg, state);

        if (status == ACCEPT) {
            state.accepted = true;
        }

        return (status);
    }

    public ClassSet<ClassDescriptor>        getSchema() {
        return (ClassSet<ClassDescriptor>) outputClassSet;
    }

    public RecordClassDescriptor []     getMessageTypes () {
        return (outputTypes);
    }

    public boolean hasWaitingMessages() {
        return waitingMessagesCount > 0;
    }

    public void updateWaitingMessages(int maxSize) {
        if (waitingMessagesCount <= 0) {
            waitingMessagesCount = maxSize;
            currentWaitingMessage = 0;
        }
    }

    public void nextWaitingMessage() {
        waitingMessagesCount--;
        if (waitingMessagesCount <= 0) {
            currentWaitingMessage = 0;
        } else {
            currentWaitingMessage++;
        }
    }

    public int currentWaitingMessage() {
        return currentWaitingMessage;
    }

    protected void setLimit(long limit, long offset) {
        if (this.limit == Long.MIN_VALUE) {
            this.limit = limit;
            this.offset = offset;
        }
    }

    protected int applyLimit(int state) {
        if (limit == Long.MIN_VALUE) {
            return state;
        }

        if (limit <= 0) {
            return ABORT;
        }

        if (state == ACCEPT) {
            if (offset > 0) {
                offset--;
                return REJECT;
            }
            limit--;
        }

        return state;
    }

    protected void prepareGroupByWarning(FilterState state) {
        if (!groupByWarningSent && state != null && state.warningMessage != null) {
            groupByWarningSent = true;
            queryStatusMessage = queryStatusMessageProvider.prepareQueryStatusMessage(
                    QueryStatus.WARNING, lastState.warningMessage
            );
        }
    }

    protected boolean hasPendingQueryStatus() {
        if (queryStatusMessage != null) {
            outMsg = queryStatusMessage;
            queryStatusMessage = null;
            return true;
        }

        return false;
    }

    private void saveFirstTimestamp() {
        if (firstMessageTimestamp == Long.MIN_VALUE && source.getMessage() != null) {
            firstMessageTimestamp = source.getMessage().getTimeStampMs();
        }
    }
}