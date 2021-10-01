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
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.groups.GroupsCountManager;
import com.epam.deltix.util.collections.ArrayEnumeration;
import com.epam.deltix.util.lang.Util;

import java.util.Enumeration;

public abstract class GroupByEntityFilterStateProvider extends FilterStateProvider {
    private FilterState state;
    private FilterState[] states = new FilterState[255];
    private int numStates = 0;
    private final FilterState tempState;

    private final GroupsCountManager countManager = new GroupsCountManager();
    private int processedUntil;
    private boolean hasUnprocessedGroups;

    public GroupByEntityFilterStateProvider(FilterIMSImpl filter) {
        super(filter);
        tempState = newState();
        startAggregate();
    }

    public void startAggregate() {
        processedUntil = numStates;
        numStates = 0;
        tempState.warningMessage = null;
        hasUnprocessedGroups = false;
    }

    public boolean hasUnprocessedGroups() {
        return hasUnprocessedGroups;
    }

    @Override
    public final FilterState getState(RawMessage msg) {
        int currentEntityIdx = filter.getSource().getCurrentEntityIndex();

        if (currentEntityIdx < processedUntil) {
            return tempState;
        }

        final int curLength = states.length;

        if (currentEntityIdx >= curLength) {
            if (!countManager.canCreateNew(numStates)) {
                hasUnprocessedGroups = true;
                tempState.warningMessage = countManager.warningCause();
                return tempState;
            }

            int newLength = Util.doubleUntilAtLeast(curLength, currentEntityIdx + 1);
            Object[] old = states;
            states = new FilterState[newLength];
            System.arraycopy(old, 0, states, 0, curLength);
            state = states[currentEntityIdx] = newState();
            numStates = currentEntityIdx + 1;
        } else {
            state = states[currentEntityIdx];
            if (state == null) {
                assert currentEntityIdx == numStates;
                state = states[currentEntityIdx] = newState();
                numStates = currentEntityIdx + 1;
            }
        }

        return state;
    }

    @Override
    public Enumeration<FilterState> getStates() {
        return new ArrayEnumeration<>(states, 0, numStates);
    }
}