package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;

/**
 *
 */
public class SelectRequiredException extends CompilationException {
    public SelectRequiredException (long location) {
        super ("SELECT expression was expected but not found", location);
    }
}
