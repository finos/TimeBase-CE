package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MIN (BOOLEAN)
 */
@Aggregate @FunctionInfo (id = "MIN", returns = "BOOLEAN?", args = { "BOOLEAN?" })
public final class MinBoolean {
    private byte        minValue = BooleanDataType.NULL;

    public byte         get () {
        return (minValue);
    }

    public void         set1 (byte v) {
        if (v != BooleanDataType.NULL && 
            (minValue == BooleanDataType.NULL || v < minValue))
            minValue = v;
    }        
}
