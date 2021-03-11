package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;

/**
 *
 */
public class EmptyProgramException extends CompilationException {
    public EmptyProgramException () {
        super ("Empty", 0);
    }
}
