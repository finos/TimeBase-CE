package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ParamSignature;

/**
 *
 */
public class ParamRef {
    public final ParamSignature         signature;
    public final int                    index;

    public ParamRef (ParamSignature signature, int index) {
        this.signature = signature;
        this.index = index;
    }        
}
