package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class IllegalDefaultValueException extends CompilationException {
    public IllegalDefaultValueException (Expression x) {
        super ("Default value specification is not allowed here.", x);
    }    
}
