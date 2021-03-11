package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;

public class IncompatibleValueException extends CompilationException {

    public IncompatibleValueException(String msg, long location) {
        super(msg, location);
    }
}
