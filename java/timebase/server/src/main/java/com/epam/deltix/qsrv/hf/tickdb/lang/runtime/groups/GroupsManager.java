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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.groups;

import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.FilterState;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.GroupByFilterState;
import com.epam.deltix.util.collections.ArrayEnumeration;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;

import java.util.Enumeration;

public class GroupsManager {

    private final ObjectToObjectHashMap<GroupByFilterState, FilterState> states = new ObjectToObjectHashMap<>();
    private final ProcessedStatesStorage processedStatesStorage = new ProcessedStatesStorage();
    private final FilterState tempState;
    private boolean hasUnprocessedGroups;

    private final GroupsCountManager countManager = new GroupsCountManager();

    public GroupsManager(FilterState tempState) {
        this.tempState = tempState;
    }

    public void startProcess() {
        countManager.startProcess();
        states.keyIterator().forEachRemaining(processedStatesStorage::add);
        states.clear();
        hasUnprocessedGroups = false;
        tempState.setWarningMessage(null);
    }

    public void stopProcess() {
        processedStatesStorage.close();
    }

    public boolean hasUnprocessedGroups() {
        return hasUnprocessedGroups;
    }

    public FilterState getState(GroupByFilterState orderByState) {
        if (processedStatesStorage.has(orderByState)) {
            return tempState;
        }

        FilterState state = states.get(orderByState, null);
        if (state == null) {
            if (!countManager.canCreateNew(states.size())) {
                hasUnprocessedGroups = true;
                tempState.setWarningMessage(countManager.warningCause());
                return tempState;
            }
        }

        return state;
    }

    public void putState(GroupByFilterState groupByFilterState, FilterState filterState) {
        states.put(groupByFilterState, filterState);
    }

    public Enumeration<FilterState> getStates() {
        return new ArrayEnumeration<>((FilterState[]) states.valuesToArray(new FilterState[states.size()]));
    }

}