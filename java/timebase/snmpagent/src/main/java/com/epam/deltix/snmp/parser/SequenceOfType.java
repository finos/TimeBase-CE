package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class SequenceOfType extends Type {
    public final String                 elementTypeId;

    public SequenceOfType (long location, String elementTypeId) {
        super (location);
        
        this.elementTypeId = elementTypeId;
    }
        
}
