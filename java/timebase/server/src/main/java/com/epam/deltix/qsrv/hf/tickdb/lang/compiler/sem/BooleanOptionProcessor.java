package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalOptionValueException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OptionElement;

/**
 *
 */
public abstract class BooleanOptionProcessor <T> extends OptionProcessor <T> {
    public BooleanOptionProcessor (
        String      key
    )
    {
        super (key, StandardTypes.CLEAN_BOOLEAN);        
    }
    
    protected abstract void     set (T target, boolean value);
    
    protected abstract boolean  get (T source);
    
    @Override
    public final void           process (OptionElement option, CompiledConstant value, T target) {
        if (value == null || value.isNull ())
            throw new IllegalOptionValueException (option, null);
            
        set (target, value.getBoolean ());
    }

    @Override
    protected void              printValue (T source, StringBuilder out) {
        out.append (get (source) ? "TRUE" : "FALSE");
    }        
}
