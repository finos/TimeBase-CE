package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class WrongNumArgsException extends CompilationException {
    public WrongNumArgsException (CallExpression e, int num) {
        super ("Function " + e.name + " () may not be applied to " + num + " arguments.", e);
    }
}
