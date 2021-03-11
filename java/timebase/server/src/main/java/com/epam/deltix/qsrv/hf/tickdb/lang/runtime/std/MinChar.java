package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MIN (CHAR)
 */
@Aggregate @FunctionInfo (id = "MIN", returns = "CHAR?", args = { "CHAR?" })
public final class MinChar {
    private char        minValue = CharDataType.NULL;

    public char         get () {
        return (minValue);
    }

    public void         set1 (char v) {
        if (v != CharDataType.NULL && 
            (minValue == CharDataType.NULL || v < minValue))
            minValue = v;
    }        
}
