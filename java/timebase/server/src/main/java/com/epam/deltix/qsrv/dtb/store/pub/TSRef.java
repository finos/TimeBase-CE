package com.epam.deltix.qsrv.dtb.store.pub;

/**
 *  Immutable reference to a TSF. The TSF to which this object refers 
 *  may disappear at any time as a result of data being written to store. 
 */
public interface TSRef {  
    public String                   getPath ();
}
