package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public interface DataFieldInfo {
    public RecordClassDescriptor    getOwner ();

    public String                   getName ();

    public String                   getTitle ();
    
    public DataType                 getType ();
}
