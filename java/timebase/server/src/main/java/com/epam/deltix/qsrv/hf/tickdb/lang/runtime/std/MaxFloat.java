package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MAX (FLOAT)
 */
@Aggregate @FunctionInfo (id = "MAX", returns = "FLOAT?", args = { "FLOAT?" })
public final class MaxFloat {
    private double      maxValue = Double.NaN;

    public double       get () {
        return (maxValue);
    }

    public void         set1 (double v) {
        if (Double.isNaN (maxValue) || v > maxValue)
            maxValue = v;
    }        
}
