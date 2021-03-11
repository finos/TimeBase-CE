package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;
import java.util.*;

/**
 *
 */
public abstract class ClassDef extends Element {
    public static ClassDef []  toArray (List <ClassDef> arrayList) {
        if (arrayList == null) 
            return (null);
        
        ClassDef []   cdArray = new ClassDef [arrayList.size ()];
        
        arrayList.toArray (cdArray);
        
        return (cdArray);
    }
    
    public final TypeIdentifier id;
    public final String         title;
    public final String         comment;

    protected ClassDef (long location, TypeIdentifier id, String title, String comment) {
        super (location);
        this.id = id;
        this.title = title;
        this.comment = comment;
    }        
    
    protected final void        printHeader (StringBuilder s) {
        id.print (s);
        
        if (title != null) {
            s.append (' ');
            GrammarUtil.escapeStringLiteral (title, s);
        }
    }
    
    protected final void        printComment (StringBuilder s) {
        if (comment != null) {
            s.append ("\nCOMMENT ");
            GrammarUtil.escapeStringLiteral (comment, s);
        }
    }
}
