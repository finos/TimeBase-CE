package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class StructureType extends Type {
    public final Field []               fields;

    public StructureType (long location, Field [] fields) {
        super (location);
        
        this.fields = fields;
    }
        
}
