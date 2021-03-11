package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public class NamedExpression extends ComplexExpression {
    public final String             name;

    public NamedExpression (long location, Expression arg, String name) {
        super (location, arg);
        this.name = name;
    }

    public NamedExpression (Expression arg, String name) {
        this (NO_LOCATION, arg, name);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        getArgument ().print (outerPriority, s);
        s.append (" AS ");
        GrammarUtil.escapeVarId (name, s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            name.equals (((NamedExpression) obj).name)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + name.hashCode ());
    }
}
