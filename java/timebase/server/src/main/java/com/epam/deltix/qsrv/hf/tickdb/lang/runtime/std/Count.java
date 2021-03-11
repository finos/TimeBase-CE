package com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Aggregate counter.
 */
@Aggregate @FunctionInfo (id = "COUNT", returns = "INTEGER", args = { })
public class Count {
    private long    n = 0;
    
    public void         update () {
        n++;
    }
    
    public long         get () {
        return (n);
    }
}
