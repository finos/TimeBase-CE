package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MAX (TIMEOFDAY)
 */
@Aggregate @FunctionInfo (id = "MAX", returns = "TIMEOFDAY?", args = { "TIMEOFDAY?" })
public final class MaxTimeOfDay {
    private int         maxValue = TimeOfDayDataType.NULL;

    public int          get () {
        return (maxValue);
    }

    public void         set1 (int v) {
        if (v > maxValue)
            maxValue = v;
    }        
}
