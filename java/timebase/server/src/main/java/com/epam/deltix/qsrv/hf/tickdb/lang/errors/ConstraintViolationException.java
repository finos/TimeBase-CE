package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;

public class ConstraintViolationException extends CompilationException {

    public ConstraintViolationException(String msg, long location) {
        super(msg, location);
    }
}
