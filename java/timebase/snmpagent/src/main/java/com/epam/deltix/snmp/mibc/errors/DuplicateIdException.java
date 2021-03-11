package com.epam.deltix.snmp.mibc.errors;

import com.epam.deltix.util.parsers.CompilationException;

/**
 *
 */
public class DuplicateIdException extends CompilationException {
    public final String             id;

    public DuplicateIdException (long location, String id) {
        super ("Descriptor " + id + " is already registered", location);
        
        this.id = id;
    }            
}
