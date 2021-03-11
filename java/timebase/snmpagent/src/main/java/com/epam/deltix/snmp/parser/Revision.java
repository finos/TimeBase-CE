package com.epam.deltix.snmp.parser;

import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public final class Revision extends Element {
    public final String                 date;
    public final String                 description;

    public Revision (long location, String date, String description) {
        super (location);
        
        this.date = date;
        this.description = description;
    }
            
}
