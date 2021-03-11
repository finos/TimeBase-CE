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
