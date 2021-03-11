package com.epam.deltix.snmp.parser;

import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public final class Field extends Element {
    public final String             name;
    public final Type               type;

    public Field (long location, String name, Type type) {
        super (location);
        
        this.name = name;
        this.type = type;
    }
    
    
}
