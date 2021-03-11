package com.epam.deltix.snmp.parser;

/**
 *
 */
public abstract class TypeDefinition extends Definition <Type> {
    public TypeDefinition (
        long            location, 
        String          id, 
        String          description,
        Type            value
    )
    {
        super (location, id, description, value);
    }    
}
