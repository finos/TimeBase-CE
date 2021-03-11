package com.epam.deltix.snmp.parser;

/**
 *
 */
public final class BitsType extends Type {
    public final NameNumberPair []              components;

    public BitsType (long location, NameNumberPair [] components) {
        super (location);
        
        this.components = components;
    }
        
}
