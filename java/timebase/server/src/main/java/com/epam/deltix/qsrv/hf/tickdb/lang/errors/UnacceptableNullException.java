package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Null;

/**
 *
 */
public class UnacceptableNullException extends CompilationException {
    public UnacceptableNullException (Null e) {
        super (
            "null is not allowed here",
            e
        );
    }
}
