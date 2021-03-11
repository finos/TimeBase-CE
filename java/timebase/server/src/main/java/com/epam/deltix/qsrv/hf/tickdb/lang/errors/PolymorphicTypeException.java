package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;

/**
 *
 */
public class PolymorphicTypeException extends CompilationException {
    public PolymorphicTypeException (Expression e, ClassDataType actual) {
        super (
            "Illegal type in: " + e +
            "; expected FIXED type; found POLYMORPHIC: " + actual,
            e
        );
    }
}
