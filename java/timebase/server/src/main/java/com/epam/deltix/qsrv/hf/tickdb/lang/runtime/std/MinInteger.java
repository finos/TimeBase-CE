package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MIN (INTEGER)
 */
@Aggregate @FunctionInfo (id = "MIN", returns = "INTEGER?", args = { "INTEGER?" })
public final class MinInteger {
    private long        minValue = IntegerDataType.INT64_NULL;

    public long         get () {
        return (minValue);
    }

    public void         set1 (long v) {
        if (v != IntegerDataType.INT64_NULL &&
            (minValue == IntegerDataType.INT64_NULL || v < minValue))
            minValue = v;
    }        
}
