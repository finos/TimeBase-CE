package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.util.lang.MathUtil;
import java.util.Comparator;

/**
 *
 */
class TDISComparator implements Comparator <RawReaderBase> {
    public static final TDISComparator  INSTANCE = new TDISComparator ();

    private TDISComparator () {
    }

    public int compare (RawReaderBase o1, RawReaderBase o2) {
        return (MathUtil.compare (o1.getCurrentOffset(), o2.getCurrentOffset()));
    }
}
