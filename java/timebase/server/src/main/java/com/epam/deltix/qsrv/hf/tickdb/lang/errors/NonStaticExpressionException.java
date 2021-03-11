package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;

/**
 *
 */
public class NonStaticExpressionException extends CompilationException {
    public NonStaticExpressionException (Expression e) {
        super (
            "The value of " + e + " cannot be computed.",
            e
        );
    }
}
