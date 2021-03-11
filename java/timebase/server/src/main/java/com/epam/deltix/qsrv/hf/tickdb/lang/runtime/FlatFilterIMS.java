package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.util.collections.ArrayEnumeration;
import com.epam.deltix.util.collections.EmptyEnumeration;
import java.util.Enumeration;

/**
 *  Does not group.
 */
public abstract class FlatFilterIMS extends FilterIMSImpl {
    private FilterState                 state;    
    
    public FlatFilterIMS (
        InstrumentMessageSource         source,
        RecordClassDescriptor []        inputTypes,
        RecordClassDescriptor []        outputTypes,
        ReadableValue []                params
    )
    {
        super (source, inputTypes, outputTypes, params);
    }

    protected final FilterState         getState (RawMessage msg) {
        if (state == null)
            state = newState ();
        
        return (state);
    }
    
    protected Enumeration <FilterState>  getStates () {        
        return (
            state == null ? 
                new EmptyEnumeration <FilterState> () : 
                new ArrayEnumeration <FilterState> (state)
        );
    }        
}
