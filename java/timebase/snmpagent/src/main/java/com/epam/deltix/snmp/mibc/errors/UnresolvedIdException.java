package com.epam.deltix.snmp.mibc.errors;

import com.epam.deltix.util.parsers.CompilationException;

/**
 *
 */
public class UnresolvedIdException extends CompilationException {
    public final String             id;

    public UnresolvedIdException (long location, String id) {
        super ("Unresolved object identifier: " + id, location);
        
        this.id = id;
    }
    
    
    
}
