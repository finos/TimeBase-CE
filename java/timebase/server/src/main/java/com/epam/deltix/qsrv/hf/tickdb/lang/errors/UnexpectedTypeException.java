package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;

/**
 *
 */
public class UnexpectedTypeException extends CompilationException {
    public UnexpectedTypeException (Expression e, DataType expected, DataType actual) {
        super (
            "Illegal type in: " + e +
            "; context requires: " + expected.getBaseName () +
            "; found: " + actual.getBaseName (),
            e
        );
    }
}
