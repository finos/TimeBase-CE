package com.epam.deltix.snmp.parser;

/**
 *
 */
public class NamedType extends Type {
    public final String                 typeId;
    
    public NamedType (
        long                            location,
        String                          typeId        
    ) 
    {
        super (location);
        
        this.typeId = typeId;
    }
               
}
