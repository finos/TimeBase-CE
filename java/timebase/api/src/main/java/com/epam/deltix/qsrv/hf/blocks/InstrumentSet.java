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


import java.util.*;

/**
 *
 */

// TODO: use ObjectHashSet and verify

public class InstrumentSet extends HashSet <IdentityKey> {
    private transient InstrumentKey buffer = new InstrumentKey ();
    
    public InstrumentSet (int initialCapacity) {
        super (initialCapacity);
    }

    public InstrumentSet () {
    }
    
    public boolean      contains (CharSequence symbol) {
        buffer.symbol = symbol;
        return (super.contains (buffer));
    }
    
    public boolean      remove (CharSequence symbol) {
        buffer.symbol = symbol;
        return (super.remove (buffer));
    }
    
    public boolean      add (CharSequence symbol) {
        return (super.add (new ConstantIdentityKey(symbol)));
    }

    @Override
    public boolean      add (IdentityKey id) {
        return (super.add (ConstantIdentityKey.makeImmutable (id)));
    }

    public boolean      addAll (IdentityKey[] ids) {
        boolean modified = false;

        for (int i = 0; i < ids.length; i++) {
            if (add(ids[i]))
                modified = true;
        }

        return modified;
    }

    public boolean      removeAll (IdentityKey[] ids) {
        boolean modified = false;

        for (int i = 0; i < ids.length; i++) {
            if (remove(ids[i]))
                modified = true;
        }

        return modified;
    }

    public IdentityKey[]      toArray () {
        return super.toArray(new IdentityKey[size()]);
    }

    @Override
    public boolean      remove (Object o) {
        if (o instanceof IdentityKey)
            return (remove ((IdentityKey) o));

        return (false);
    }

    public boolean      remove (IdentityKey id) {
        return (remove (id.getSymbol ()));
    }

    @Override
    public boolean      contains (Object o) {
        if (o instanceof IdentityKey)
            return (contains ((IdentityKey) o));
        
        return (false);
    }

    public boolean      contains (IdentityKey id) {
        return (contains (id.getSymbol ()));
    }
}