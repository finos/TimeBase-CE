package com.epam.deltix.qsrv.hf.pub.codec.validerrors;

import com.epam.deltix.qsrv.hf.pub.codec.*;

/**
 *
 */
public abstract class ValidationError {
    /**
     *  The field that failed validation, or null.
     */
    public final NonStaticFieldInfo         fieldInfo;
    
    /**
     *  Actual offset into RawMessage.data (not corrected for start offset).
     */
    public final int                        atOffset;
    
    protected ValidationError (
        int                         atOffset,
        NonStaticFieldInfo          fieldInfo        
    )
    {
        this.atOffset = atOffset;
        this.fieldInfo = fieldInfo;
    }           
}
