package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class CastExpression extends ComplexExpression {
    public final String             typeId;

    public CastExpression (long location, Expression arg, String typeId) {
        super (location, arg);
        this.typeId = typeId;
    }

    public CastExpression (Expression arg, String typeId) {
        this (NO_LOCATION, arg, typeId);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        s.append ("cast (");
        
        args [0].print (OpPriority.COMMA, s);
        
        s.append (", ");
        
        GrammarUtil.escapeIdentifier (NamedObjectType.TYPE, typeId, s);
        
        s.append (")");
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            typeId.equals (((CastExpression) obj).typeId)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + typeId.hashCode ());
    }
}
