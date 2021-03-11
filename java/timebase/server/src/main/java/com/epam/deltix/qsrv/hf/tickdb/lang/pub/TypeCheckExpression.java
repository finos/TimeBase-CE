package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public class TypeCheckExpression extends ComplexExpression {
    public final TypeIdentifier            typeId;

    public TypeCheckExpression (long location, Expression arg, TypeIdentifier typeId) {
        super (location, arg);
        this.typeId = typeId;
    }

    public TypeCheckExpression (Expression arg, TypeIdentifier typeId) {
        super (NO_LOCATION, arg);
        this.typeId = typeId;
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        getArgument ().print (outerPriority, s);
        s.append (" IS ");
        typeId.print (s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            typeId.equals (((TypeCheckExpression) obj).typeId)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + typeId.hashCode ());
    }
}
