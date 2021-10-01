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
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.groups.GroupsManager;

import java.util.Enumeration;

public abstract class GroupByFilterStateProvider extends FilterStateProvider {

    private GroupsManager groupsManager;

    public GroupByFilterStateProvider(FilterIMSImpl filter) {
        super(filter);
        groupsManager = new GroupsManager(newState());
        startAggregate();
    }

    public void startAggregate() {
        groupsManager.startProcess();
    }

    public void stopAggregate() {
        groupsManager.stopProcess();
    }

    public boolean hasUnprocessedGroups() {
        return groupsManager.hasUnprocessedGroups();
    }

    @Override
    public FilterState getState(RawMessage msg) {
        GroupByFilterState orderByState = getGroupByState(msg);
        FilterState state = groupsManager.getState(orderByState);
        if (state == null) {
            groupsManager.putState(orderByState.copy(newGroupByState()), state = newState());
        }

        return state;
    }

    @Override
    public Enumeration<FilterState> getStates() {
        return groupsManager.getStates();
    }

    protected abstract GroupByFilterState newGroupByState();

    protected GroupByFilterState getGroupByState(RawMessage msg) {
        // todo: check return value
        filter.acceptInterimState(msg);
        return (GroupByFilterState) filter.getInterimState();
    }

}