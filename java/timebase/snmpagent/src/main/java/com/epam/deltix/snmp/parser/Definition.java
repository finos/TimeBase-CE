package com.epam.deltix.snmp.parser;

import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public abstract class Definition <V> extends Element {
    public final String             id;
    public final String             description;
    public final V                  value;
    
    protected Definition (
        long                        location, 
        String                      id, 
        String                      description, 
        V                           value
    ) 
    {
        super (location);
        
        this.id = id;
        this.description = description;
        this.value = value;
    }    
}
