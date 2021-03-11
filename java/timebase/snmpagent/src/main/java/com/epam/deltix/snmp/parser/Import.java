package com.epam.deltix.snmp.parser;

import com.epam.deltix.util.parsers.Element;
import java.util.*;

/**
 *
 */
public class Import extends Element {
    public final String                 moduleId;
    public final ArrayList <String>     symbols;

    public Import (
        long                            location, 
        String                          moduleId,
        ArrayList <String>              symbols
    ) 
    {
        super (location);
        
        this.moduleId = moduleId;
        this.symbols = symbols;
    }        
}
