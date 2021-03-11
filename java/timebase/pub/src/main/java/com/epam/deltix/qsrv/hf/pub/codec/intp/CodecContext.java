package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;

/**
 *
 */
class CodecContext {
    public final double []     doubleBaseValues;
    public final float []      floatBaseValues;
    
    CodecContext (RecordLayout layout) {
        int     n = layout.getNumDoubleBaseFields ();
        
        doubleBaseValues = n == 0 ? null : new double [n];
        
        n = layout.getNumFloatBaseFields ();
        
        floatBaseValues = n == 0 ? null : new float [n];
    }
}
