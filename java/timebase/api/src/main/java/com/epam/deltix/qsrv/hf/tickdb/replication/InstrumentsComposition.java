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
package com.epam.deltix.qsrv.hf.tickdb.replication;

import com.epam.deltix.qsrv.hf.blocks.InstrumentSet;
import com.epam.deltix.timebase.messages.IdentityKey;

import java.util.Arrays;

/**
 *
 */
public class InstrumentsComposition {

    private final InstrumentSet     set = new InstrumentSet();
    public long                     timestamp = Long.MIN_VALUE;

    public InstrumentsComposition() {
    }

    public void                     add(IdentityKey[] ids) {
        set.addAll(Arrays.asList(ids));
    }

    public void                     add(IdentityKey id) {
        set.add(id);
    }

    public IdentityKey[]     list() {
        return set.toArray(new IdentityKey[set.size()]);
    }
}