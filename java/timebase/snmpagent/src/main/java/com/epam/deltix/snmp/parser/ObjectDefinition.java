package com.epam.deltix.snmp.parser;

/**
 *
 */
public abstract class ObjectDefinition extends Definition <OIDValue> {
    public ObjectDefinition (
        long            location, 
        String          id, 
        String          description,
        OIDValue        value
    )
    {
        super (location, id, description, value);
    }    
}
