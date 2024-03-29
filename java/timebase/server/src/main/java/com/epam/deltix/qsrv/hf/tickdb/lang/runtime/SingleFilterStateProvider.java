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

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.util.collections.ArrayEnumeration;
import com.epam.deltix.util.collections.EmptyEnumeration;

import java.util.Enumeration;

public abstract class SingleFilterStateProvider extends FilterStateProvider {
    protected FilterState state;

    public SingleFilterStateProvider(FilterIMSImpl filter) {
        super(filter);
    }

    @Override
    public FilterState getState(RawMessage msg) {
        if (state == null)
            state = newState();

        return (state);
    }

    @Override
    public Enumeration<FilterState> getStates() {
        return (
            state == null ?
                new EmptyEnumeration<>() :
                new ArrayEnumeration<>(state)
        );
    }
}