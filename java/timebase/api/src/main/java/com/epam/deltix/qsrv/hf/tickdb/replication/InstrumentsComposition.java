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
