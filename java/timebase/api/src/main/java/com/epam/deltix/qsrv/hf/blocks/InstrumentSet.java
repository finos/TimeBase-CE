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
