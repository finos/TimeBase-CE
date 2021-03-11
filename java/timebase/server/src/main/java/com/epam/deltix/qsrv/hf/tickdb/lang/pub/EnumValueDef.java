package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class EnumValueDef extends Element {
    public final Identifier     id;
    public final Expression     value;

    public EnumValueDef (long location, Identifier id, Expression value) {
        super (location);
        this.id = id;
        this.value = value;
    }

    public EnumValueDef (Identifier id, Expression value) {
        this (Location.NONE, id, value);
    }
    
    @Override
    public void             print (StringBuilder s) {
        id.print (s);
        
        if (value != null) {
            s.append (" = ");
            value.print (s);
        }
    }        
}
