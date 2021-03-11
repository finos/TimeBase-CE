package com.epam.deltix.snmp.parser;

import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public final class OIDValue extends Element {
    public NameNumberPair []             components;

    public OIDValue (long location, NameNumberPair [] components) {
        super (location);
        
        this.components = components;
    }        
}
