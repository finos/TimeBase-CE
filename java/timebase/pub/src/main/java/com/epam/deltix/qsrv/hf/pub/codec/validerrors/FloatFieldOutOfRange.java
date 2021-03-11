package com.epam.deltix.qsrv.hf.pub.codec.validerrors;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;

/**
 *
 */
public final class FloatFieldOutOfRange extends ValidationError {
    public final double               value;
    public final double               min;
    public final double               max;
    
    public FloatFieldOutOfRange (
        int                         atOffset,        
        NonStaticFieldInfo          fieldInfo,
        double                      value,
        double                      min,
        double                      max
    )
    {
        super (atOffset, fieldInfo);
        
        this.value = value;
        this.min = min;
        this.max = max;
    }        
}
