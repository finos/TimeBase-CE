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
package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentKey;
import com.epam.deltix.util.collections.generated.ObjectToIntegerHashMap;

/**
 *
 */
public final class InstrumentIndex {
    private final InstrumentKey buffer = new InstrumentKey ();
    private final ObjectToIntegerHashMap <IdentityKey>   map;

    public InstrumentIndex (int initialCapacity) {
        map = new ObjectToIntegerHashMap <IdentityKey> (initialCapacity);
    }

    public InstrumentIndex () {
        this (16);
    }

    private int         getOrAddFromBuffer () {
        int                 idx = map.get (buffer, -1);

        if (idx == -1) {
            idx = map.size ();
            ConstantIdentityKey key = new ConstantIdentityKey (buffer);
            map.put (key, idx);
        }

        return (idx);
    }

    public int          getOrAdd (IdentityKey id) {
        return (getOrAdd (id.getSymbol ()));
    }

    public int          getOrAdd (CharSequence symbol) {
        buffer.symbol = symbol;

        return (getOrAddFromBuffer ());
    }

    public int          size () {
        return (map.size ());
    }
}