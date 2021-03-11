package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public class CallExpression extends ComplexExpression {
    public final String             name;

    public CallExpression (long location, String name, Expression ... args) {
        super (location, args);
        this.name = name;
    }

    public CallExpression (String name, Expression ... args) {
        this (NO_LOCATION, name, args);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        int     n = args.length;
        
        GrammarUtil.escapeIdentifier (NamedObjectType.FUNCTION, name, s);
        s.append (" (");
        printCommaSepArgs (0, args.length, s);
        s.append (")");
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            name.equals (((CallExpression) obj).name)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + name.hashCode ());
    }
}
