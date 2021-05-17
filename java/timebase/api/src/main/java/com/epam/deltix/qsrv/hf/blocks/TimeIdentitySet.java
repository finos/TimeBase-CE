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
import com.epam.deltix.util.collections.CharSequenceToObjectMap;

import java.util.Iterator;

public class TimeIdentitySet<T extends TimeIdentity> implements TimeIdentity, Iterable<T> {

    private final CharSequenceToObjectMap<T> map = new CharSequenceToObjectMap<T>();
    private T                              entry;    

    public TimeIdentitySet(T entry) {
        this.entry = entry;
    }

    @SuppressWarnings ("unchecked")
    public TimeIdentity get(IdentityKey id) {

        T instance = map.get(id.getSymbol());
        
        if (instance == null) {
            entry = instance = (T) entry.create(id);
            map.put(id.getSymbol(), instance);
        }

        return instance;
    }

    @Override
    public TimeIdentity create(IdentityKey id) {
        return entry.create(id);
    }

    @Override
    public long getTime() {
        return entry.getTime();
    }

    @Override
    public void setTime(long timestamp) {
        entry.setTime(timestamp);
    }

    @Override
    public CharSequence getSymbol() {
        return entry.getSymbol();
    }

    @Override
    public Iterator<T> iterator() {
        return map.values().iterator();
    }
}
