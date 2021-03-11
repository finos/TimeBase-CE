package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class AmbiguousTypeException extends CompilationException {
    public AmbiguousTypeException (
        Expression          e,
        TypeIdentifier      typeId,
        ClassDataType       actualType
    )
    {
        super ("Type name " + typeId + " is ambiguous in the context of " + actualType, e);
    }
}
