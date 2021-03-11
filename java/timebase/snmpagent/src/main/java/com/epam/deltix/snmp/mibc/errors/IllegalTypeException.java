package com.epam.deltix.snmp.mibc.errors;

import com.epam.deltix.snmp.parser.*;
import com.epam.deltix.util.parsers.CompilationException;

/**
 *
 */
public class IllegalTypeException extends CompilationException {
    public IllegalTypeException (Type type) {
        super ("This type is not supported by SMIv2", type.location);        
    }            
}
