package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class IllegalObjectException extends CompilationException {
    public IllegalObjectException (Identifier id, Object obj) {
        super ("Object " + obj + " identified by " + id + " is illegal here.", id);
    }
    
    public IllegalObjectException (TypeIdentifier id, Object obj) {
        super ("Type " + obj + " identified by " + id + " is illegal here.", id);
    }
}
