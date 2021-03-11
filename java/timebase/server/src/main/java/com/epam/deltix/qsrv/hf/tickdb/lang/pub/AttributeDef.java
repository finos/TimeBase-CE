package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public abstract class AttributeDef extends Element {
    public final String         id;
    public final String         title;
    public final String         comment;
    
    protected AttributeDef (String id, String title, String comment, long location) {
        super (location);
        this.id = id;
        this.title = title;
        this.comment = comment;
    }
    
    protected final void        printHeader (StringBuilder s) {
        GrammarUtil.escapeVarId (id, s);
        
        if (title != null) {
            s.append (' ');
            GrammarUtil.escapeStringLiteral (title, s);
        }
    }
    
    protected final void        printComment (StringBuilder s) {
        if (comment != null) {
            s.append (" COMMENT ");
            GrammarUtil.escapeStringLiteral (comment, s);
        }
    }
}
