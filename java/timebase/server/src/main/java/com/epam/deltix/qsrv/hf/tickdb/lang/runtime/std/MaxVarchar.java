package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.Varchar;
import com.epam.deltix.util.lang.Util;

/**
 *  Aggregate MAX (VARCHAR)
 */
@Aggregate @FunctionInfo (id = "MAX", returns = "VARCHAR?", args = { "VARCHAR?" })
public final class MaxVarchar {
    private Varchar         maxValue = new Varchar ();

    public CharSequence     get () {
        return (maxValue.get ());
    }

    public void         set1 (CharSequence v) {
        CharSequence        mvcs = maxValue.get ();
                
        if (mvcs == null || Util.compare (v, mvcs, false) > 0)
            maxValue.set (v);
    }        
}
