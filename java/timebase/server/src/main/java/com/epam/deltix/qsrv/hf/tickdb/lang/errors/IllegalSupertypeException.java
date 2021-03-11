package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TypeIdentifier;

/**
 *
 */
public class IllegalSupertypeException extends CompilationException {
    public IllegalSupertypeException (TypeIdentifier id) {
        super ("Illegal supertype " + id, id);
    }
}
