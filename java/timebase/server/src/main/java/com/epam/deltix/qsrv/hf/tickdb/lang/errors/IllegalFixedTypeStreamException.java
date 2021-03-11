package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class IllegalFixedTypeStreamException extends CompilationException {
    public IllegalFixedTypeStreamException (
        OptionElement               option, 
        int                         numConcreteClasses
    )
    {
        super (
            "A fixedType stream must have exactly one non-instantiable class "
                + "defined. Found " + numConcreteClasses, 
            option
        );
    }        
}
