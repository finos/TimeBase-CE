package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class DropStreamStatement extends Statement {
    public final Identifier        id;
    
    public DropStreamStatement (long location, Identifier id) {
        super (location);
        
        this.id = id;        
    }
    
    public DropStreamStatement (Identifier id) {
        this (Location.NONE, id);
    }
    
    @Override
    public void                     print (StringBuilder s) {
        s.append ("DROP STREAM ");
        id.print (s);        
    }        
}
