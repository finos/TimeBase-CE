package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;

/**
 *
 */
public class ClassTypeExpectedException extends CompilationException {
    public ClassTypeExpectedException (Expression e, DataType actual) {
        super (
            "Illegal type in: " + e +
            "; expected: CLASS; found: " + actual.getBaseName (),
            e
        );
    }
}
