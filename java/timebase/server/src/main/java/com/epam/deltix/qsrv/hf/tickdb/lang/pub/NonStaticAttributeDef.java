package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public class NonStaticAttributeDef extends AttributeDef {
    public final DataTypeSpec       type;
    public final Identifier         relativeId;
    public final Expression         defval;
    
    public NonStaticAttributeDef (
        long                    location, 
        String                  id,
        String                  title,
        String                  comment, 
        DataTypeSpec            type,
        Identifier              relativeId,
        Expression              defval
    )
    {
        super (id, title, comment, location);
        this.type = type;
        this.relativeId = relativeId;
        this.defval = defval;
    }

    public NonStaticAttributeDef (
        String                  id,
        String                  title,
        String                  comment, 
        DataTypeSpec            type,
        Identifier              relativeId,
        Expression              defval
    )
    {
        this (Location.NONE, id, title, comment, type, relativeId, defval);
    }

    @Override
    public void print (StringBuilder s) {
        printHeader (s);
        s.append (' ');
        type.print (s);
        
        if (relativeId != null) {
            s.append (" RELATIVE TO ");
            relativeId.print (s);
        }
        
        if (defval != null) {
            s.append (" DEFAULT ");
            defval.print (s);
        }
        
        printComment (s);
    }        
}
