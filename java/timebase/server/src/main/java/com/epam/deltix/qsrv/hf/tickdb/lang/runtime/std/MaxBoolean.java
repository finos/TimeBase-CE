package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MAX (BOOLEAN)
 */
@Aggregate @FunctionInfo (id = "MAX", returns = "BOOLEAN?", args = { "BOOLEAN?" })
public final class MaxBoolean {
    private byte        maxValue = BooleanDataType.NULL;

    public byte         get () {
        return (maxValue);
    }

    public void         set1 (byte v) {
        if (v > maxValue)
            maxValue = v;
    }        
}
