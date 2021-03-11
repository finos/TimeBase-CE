package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MIN (FLOAT)
 */
@Aggregate @FunctionInfo (id = "MIN", returns = "FLOAT?", args = { "FLOAT?" })
public final class MinFloat {
    private double      minValue = Double.NaN;

    public double       get () {
        return (minValue);
    }

    public void         set1 (double v) {
        if (!Double.isNaN (v) && 
            (Double.isNaN (minValue) || v < minValue))
            minValue = v;
    }        
}
