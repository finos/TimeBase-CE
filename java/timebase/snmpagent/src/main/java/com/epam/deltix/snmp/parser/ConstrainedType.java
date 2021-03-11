package com.epam.deltix.snmp.parser;

/**
 *
 */
public class ConstrainedType extends Type {
    public final Type                   baseType;
    public final Constraint             constraint;

    public ConstrainedType (
        long                            location,
        Type                            baseType, 
        Constraint                      constraint       
    ) 
    {
        super (location);
        
        this.baseType = baseType;
        this.constraint = constraint;
    }               
}
