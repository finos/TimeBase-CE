package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *
 */
public class DuplicateIdentifierException extends CompilationException {
    public DuplicateIdentifierException (String id, long location) {
        super ("Duplicate object: " + id, location);
    }
    
    public DuplicateIdentifierException (Identifier id) {
        super ("Duplicate object: " + id.id, id);
    }
    
    public DuplicateIdentifierException (TypeIdentifier id) {
        super ("Duplicate type: " + id.typeName, id);
    }
}
