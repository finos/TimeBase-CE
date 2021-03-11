package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class StaticAttributeDef extends AttributeDef {
    public final TypeIdentifier     typeId;
    public final Expression         value;
    
    public StaticAttributeDef (
        long                        location,
        String                      id,
        String                      title, 
        String                      comment,
        TypeIdentifier              typeId,
        Expression                  value
    ) 
    {
        super (id, title, comment, location);
        
        this.typeId = typeId;
        this.value = value;
    }
    
    public StaticAttributeDef (
        String                      id,
        String                      title, 
        String                      comment,
        TypeIdentifier              typeId,
        Expression                  value
    ) 
    {
        this (Location.NONE, id, title, comment, typeId, value);
    }

    @Override
    public void         print (StringBuilder s) {
        s.append ("STATIC ");
        printHeader (s);
        s.append (' ');
        typeId.print (s);
        s.append (" = ");
        value.print (s);
        printComment (s);
    }        
}
