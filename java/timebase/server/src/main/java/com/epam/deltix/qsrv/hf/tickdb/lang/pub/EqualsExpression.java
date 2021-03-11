package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class EqualsExpression extends ComplexExpression {
    public final boolean        isEqual;

    public EqualsExpression (long location, Expression left, Expression right, boolean isEqual) {
        super (location, left, right);

        this.isEqual = isEqual;
    }

    public EqualsExpression (Expression left, Expression right, boolean isEqual) {
        this (NO_LOCATION, left, right, isEqual);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        printBinary (
            outerPriority, isEqual ? " = " : " != ",
            OpPriority.RELATIONAL,
            InfixAssociation.LEFT,
            s
        );
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            isEqual == ((EqualsExpression) obj).isEqual
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * (isEqual ? 23 : 41));
    }
}
