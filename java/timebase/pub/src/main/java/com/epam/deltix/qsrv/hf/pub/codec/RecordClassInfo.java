package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

/**
 *
 */
public interface RecordClassInfo {
    public RecordClassDescriptor    getDescriptor ();
    
    public StaticFieldInfo []       getStaticFields ();
    
    public NonStaticFieldInfo []    getNonStaticFields ();
    
    public NonStaticFieldInfo []    getPrimaryKeyFields ();
    
    public DataFieldInfo            getField (String name);

    public Object                   newInstance ();

    public Class<?>                 getTargetClass ();
}
