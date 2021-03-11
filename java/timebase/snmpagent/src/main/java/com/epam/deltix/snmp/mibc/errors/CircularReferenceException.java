package com.epam.deltix.snmp.mibc.errors;

import com.epam.deltix.snmp.parser.Definition;
import com.epam.deltix.util.parsers.CompilationException;

/**
 *
 */
public class CircularReferenceException extends CompilationException {
    public final String             id;

    public CircularReferenceException (Definition <?> def) {
        super ("Descriptor " + def.id + " is involved in a circular dependency", def.location);
        
        this.id = def.id;
    }            
}
