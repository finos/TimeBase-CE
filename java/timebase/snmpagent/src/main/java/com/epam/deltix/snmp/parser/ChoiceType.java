package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class ChoiceType extends Type {
    public final Field []               fields;

    public ChoiceType (long location, Field [] fields) {
        super (location);
        
        this.fields = fields;
    }
        
}
