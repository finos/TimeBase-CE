package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public class Identifier extends Expression {
    public String                   id;

    public Identifier (long location, String id) {
        super (location);
        this.id = id;
    }

    public Identifier (String id) {
        this (NO_LOCATION, id);
    }

    @Override
    protected void          print (int outerPriority, StringBuilder s) {
        GrammarUtil.escapeVarId (id, s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            id.equals (((Identifier) obj).id)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + id.hashCode ());
    }
}
