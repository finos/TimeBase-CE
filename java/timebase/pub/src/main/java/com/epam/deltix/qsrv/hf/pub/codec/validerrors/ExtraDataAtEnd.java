package com.epam.deltix.qsrv.hf.pub.codec.validerrors;

/**
 *
 */
public final class ExtraDataAtEnd extends ValidationError {    
    public ExtraDataAtEnd (
        int                         atOffset
    )
    {
        super (atOffset, null);        
    }        
}
