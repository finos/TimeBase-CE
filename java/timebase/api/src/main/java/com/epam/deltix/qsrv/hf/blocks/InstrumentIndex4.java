package com.epam.deltix.qsrv.hf.blocks;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.collections.CharSequenceToIntegerMap;

/**
 *
 */
public final class InstrumentIndex4 {
    //private final InstrumentKey buffer = new InstrumentKey ();
    private final CharSequenceToIntegerMap map;

    public InstrumentIndex4(int initialCapacity) {
        map = new CharSequenceToIntegerMap(initialCapacity);
    }

    public InstrumentIndex4() {
        this (16);
    }

    public int getOrAdd(CharSequence symbol) {
        int idx = map.get(symbol, -1);

        if (idx == -1) {
            idx = map.size();
            map.put(symbol, idx);
        }

        return (idx);
    }

    public int          getOrAdd (IdentityKey id) {
        return (getOrAdd (id.getSymbol ()));
    }

    public int          size () {
        return (map.size ());
    }
}
