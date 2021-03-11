package com.epam.deltix.snmp.parser;

import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public final class Module extends Element {
    public final String             id;
    public final Import []          imports;
    public final Definition []      definitions;

    public Module (
        long                    location, 
        String                  id,
        Import []               imports,
        Definition []           definitions
    ) 
    {
        super (location);
        this.id = id;
        this.imports = imports;
        this.definitions = definitions;
    }
    
    
}
