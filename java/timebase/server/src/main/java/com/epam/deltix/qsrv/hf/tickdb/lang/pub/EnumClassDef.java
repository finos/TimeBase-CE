package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class EnumClassDef extends ClassDef {
    public final boolean            isFlags;
    public final EnumValueDef []    values;

    public EnumClassDef (
        long                location,
        TypeIdentifier      id, 
        String              title, 
        String              comment,
        boolean             isFlags, 
        EnumValueDef ...    values        
    )
    {
        super (location, id, title, comment);
        this.isFlags = isFlags;
        this.values = values;
    }
    
    public EnumClassDef (
        TypeIdentifier      id, 
        String              title, 
        String              comment,
        boolean             isFlags, 
        EnumValueDef ...    values        
    )
    {
        this (Location.NONE, id, title, comment, isFlags, values);
    }
    
    @Override
    public void             print (StringBuilder s) {
        s.append ("ENUM ");
        printHeader (s);
        s.append (" (");
        
        boolean     first = true;
        
        for (EnumValueDef vd : values) {
            if (first) {
                first = false;
                s.append ('\n');
            }
            else
                s.append (",\n");
            
            vd.print (s);
        }
        
        s.append ("\n)");
        printComment (s);
    }
}
