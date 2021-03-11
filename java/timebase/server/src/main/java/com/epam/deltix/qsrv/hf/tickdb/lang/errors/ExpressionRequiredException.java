package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;

/**
 *
 */
public class ExpressionRequiredException extends CompilationException {
    public ExpressionRequiredException (long location) {
        super ("An expression was expected but not found", location);
    }
}
