package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.Varchar;
import com.epam.deltix.util.lang.Util;

/**
 *  Aggregate MIN (VARCHAR)
 */
@Aggregate @FunctionInfo (id = "MIN", returns = "VARCHAR?", args = { "VARCHAR?" })
public final class MinVarchar {
    private Varchar         minValue = new Varchar ();

    public CharSequence     get () {
        return (minValue.get ());
    }

    public void         set1 (CharSequence v) {
        CharSequence        mvcs = minValue.get ();
                
        if (v != null &&
            (mvcs == null || Util.compare (v, mvcs, false) < 0))
            minValue.set (v);
    }        
}
