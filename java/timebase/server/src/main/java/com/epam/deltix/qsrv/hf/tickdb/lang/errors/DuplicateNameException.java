package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class DuplicateNameException extends CompilationException {
    public DuplicateNameException (
        Expression          e,
        String              name
    )
    {
        super ("Duplicate name " + name + " in " + e, e);
    }
}
