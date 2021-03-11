package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class ObjectTypeDefinition extends ObjectDefinition {
    public final Type               syntax;
    public final String             units;
    public final String             maxAccess;
    public final String             status;
    public final String             reference;
    public final IndexInfo          index;
    
    public ObjectTypeDefinition (
        long                        location, 
        String                      id,
        Type                        syntax,
        String                      units, 
        String                      maxAccess, 
        String                      status, 
        String                      description, 
        String                      reference, 
        IndexInfo                   index,
        OIDValue                    value
    ) 
    {
        super (location, id, description, value);
        
        this.syntax = syntax;
        this.units = units;
        this.maxAccess = maxAccess;
        this.status = status;
        this.reference = reference;
        this.index = index;
    }           
}
