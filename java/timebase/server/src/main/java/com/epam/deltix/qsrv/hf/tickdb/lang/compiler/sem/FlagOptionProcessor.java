package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalOptionValueException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OptionElement;

/**
 *
 */
public abstract class FlagOptionProcessor <T> extends OptionProcessor <T> {
    public FlagOptionProcessor (String key) {
        super (key, null);
    }
    
    protected abstract void     set (OptionElement option, T target);
    
    @Override
    public final void           process (OptionElement option, CompiledConstant value, T target) {
        if (value != null && !value.isNull ())
            throw new IllegalOptionValueException (option, value);
            
        set (option, target);
    }
}
