package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Expression;

/**
 *
 */
public class IllegalMessageSourceException extends CompilationException {
    public IllegalMessageSourceException (Expression e) {
        super ("Illegal message source: " + e, e);
    }
}
