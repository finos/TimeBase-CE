package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MAX (INTEGER)
 */
@Aggregate @FunctionInfo (id = "MAX", returns = "INTEGER?", args = { "INTEGER?" })
public final class MaxInteger {
    private long        maxValue = IntegerDataType.INT64_NULL;

    public long         get () {
        return (maxValue);
    }

    public void         set1 (long v) {
        if (v > maxValue)
            maxValue = v;
    }        
}
