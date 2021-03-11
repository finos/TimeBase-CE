package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.util.lang.Disposable;

/**
 *  Analogous to JDBC's PreparedStatement
 */
public interface PreparedQuery extends Disposable {
    public boolean                      isReverse ();
    
    public InstrumentMessageSource      executeQuery (
        SelectionOptions                    options,
        ReadableValue []                    params
    );
}
