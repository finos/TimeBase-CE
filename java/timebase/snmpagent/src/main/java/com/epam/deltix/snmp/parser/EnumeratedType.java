package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class EnumeratedType extends Type {
    public final NameNumberPair []              components;

    public EnumeratedType (long location, NameNumberPair [] components) {
        super (location);
        
        this.components = components;
    }
        
}
