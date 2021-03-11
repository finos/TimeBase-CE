package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;

/**
 *
 */
class ModifyFieldData {
    public final CompiledConstant       defValue;
    public final long                   location;

    public ModifyFieldData (CompiledConstant defValue, long location) {
        this.defValue = defValue;
        this.location = location;
    }        
}
