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
package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentKey;

import java.util.*;

/**
 * Efficiently maps instrument identity (not necessarily immutable) to 
 * arbitrary objects. The methods can be called directly on messages or 
 * mutable buffers. As all collections, this class requires external 
 * synchronization.
 */
public class InstrumentToObjectMap <T> implements Map<IdentityKey, T>{
    private InstrumentKey buffer = new InstrumentKey ();

    private final HashMap <IdentityKey, T> map;

    public InstrumentToObjectMap (int initialCapacity) {
        map = new HashMap<IdentityKey, T>(initialCapacity);
    }

    public InstrumentToObjectMap () {
        map = new HashMap<IdentityKey, T>();
    }

    public T            put (CharSequence symbol, T value) {
        return _put (new ConstantIdentityKey(symbol), value);
    }

    public T            put (IdentityKey key, T value) {
        if (key instanceof ConstantIdentityKey)
            return _put((ConstantIdentityKey)key, value);

        return _put (new ConstantIdentityKey (key.getSymbol()), value);
    }

    private T            _put (ConstantIdentityKey key, T value) {
        return map.put(key, value);
    }

    public T            get (CharSequence symbol) {
        buffer.symbol = symbol;
        return map.get (buffer);
    }

    public T            get (IdentityKey iid) {
        buffer.symbol = iid.getSymbol();
        return map.get (buffer);
    }

    @Override
    public T get(Object key) {
        if (key instanceof IdentityKey)
            return get ((IdentityKey) key);

        return null;
    }

    public T remove(IdentityKey iid) {
        buffer.symbol = iid.getSymbol();
        return map.remove (buffer);
    }

    public T            remove (CharSequence symbol) {
        buffer.symbol = symbol;
        return map.remove (buffer);
    }

    @Override
    public T            remove (Object key) {
        if (key instanceof IdentityKey)
            return remove((IdentityKey) key);

        return null;
    }


    @Override
    public boolean containsKey(Object key) {
        if (key instanceof IdentityKey)
            return containsKey((IdentityKey) key);

        return false;
    }

    public boolean containsKey(IdentityKey iid) {
        buffer.symbol = iid.getSymbol();
        return map.containsKey(buffer);
    }

    public boolean containsKey(CharSequence symbol) {
        buffer.symbol = symbol;
        return map.containsKey(buffer);
    }

    @Override
    @SuppressWarnings ("element-type-mismatch")
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Set<java.util.Map.Entry<IdentityKey, T>> entrySet() {
        return map.entrySet();
    }


    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Set<IdentityKey> keySet() {
        return map.keySet();
    }

    @Override
    public void putAll(Map<? extends IdentityKey, ? extends T> m) {
        for (Map.Entry<? extends IdentityKey, ? extends T> entry : m.entrySet()) {
            put (entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Collection<T> values() {
        return map.values();
    }
}