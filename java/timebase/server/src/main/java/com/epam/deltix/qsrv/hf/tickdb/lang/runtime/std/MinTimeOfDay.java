package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.pub.md.TimeOfDayDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate MIN (TIMEOFDAY)
 */
@Aggregate @FunctionInfo (id = "MIN", returns = "TIMEOFDAY?", args = { "TIMEOFDAY?" })
public final class MinTimeOfDay {
    private int         minValue = TimeOfDayDataType.NULL;

    public int          get () {
        return (minValue);
    }

    public void         set1 (int v) {
        if (v != TimeOfDayDataType.NULL &&
            (minValue == TimeOfDayDataType.NULL || v < minValue))
            minValue = v;
    }        
}
