package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;

/**
 *
 */
public class IllegalTypeCombinationException extends CompilationException {
    public IllegalTypeCombinationException (Expression e, DataType t1, DataType t2) {
        super (
            "Illegal combination of types in " + e +
                ": " + t1.getBaseName () + " is not compatible with " +
                t2.getBaseName (),
            e
        );
    }
}
