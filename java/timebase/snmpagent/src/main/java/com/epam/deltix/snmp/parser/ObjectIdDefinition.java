package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class ObjectIdDefinition extends ObjectDefinition {
    public final String             status;
    public final String             reference;
    
    public ObjectIdDefinition (
        long                        location, 
        String                      id, 
        String                      status,
        String                      description,
        String                      reference,
        OIDValue                    value
    )
    {
        super (location, id, description, value);
        
        this.status = status;
        this.reference = reference;
    }    
}
