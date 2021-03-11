package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.StandardTypes;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.IllegalOptionValueException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OptionElement;

/**
 *
 */
public abstract class IntegerOptionProcessor <T> extends OptionProcessor <T> {
    public final Long           minInclusive;
    public final Long           maxInclusive;

    public IntegerOptionProcessor (
        String      key, 
        Long        minInclusive, 
        Long        maxInclusive
    )
    {
        super (key, StandardTypes.CLEAN_INTEGER);
        
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }
    
    protected abstract void     set (T target, long value);
    
    @Override
    public final void           process (OptionElement option, CompiledConstant value, T target) {
        if (value == null || value.isNull ())
            throw new IllegalOptionValueException (option, null);
            
        long            n = value.getLong ();

        if (minInclusive != null && n < minInclusive || 
            maxInclusive != null && n > maxInclusive)
            throw new IllegalOptionValueException (option, n, minInclusive, maxInclusive);

        set (target, n);
    }
}
