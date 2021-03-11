package com.epam.deltix.qsrv.hf.pub.codec.validerrors;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;

/**
 *
 */
public final class IntegerFieldOutOfRange extends ValidationError {
    public final long               value;
    public final long               min;
    public final long               max;
    
    public IntegerFieldOutOfRange (
        int                         atOffset,        
        NonStaticFieldInfo          fieldInfo,
        long                        value,
        long                        min,
        long                        max
    )
    {
        super (atOffset, fieldInfo);
        
        this.value = value;
        this.min = min;
        this.max = max;
    }        
}
