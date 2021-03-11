package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public class LikeExpression extends ComplexExpression {
    public final boolean isNegative;

    public LikeExpression (long location, Expression left, Expression right, boolean isNegative) {
        super (location, left, right);

        this.isNegative = isNegative;
    }

    public LikeExpression (Expression left, Expression right, boolean isNegative) {
        this (NO_LOCATION, left, right, isNegative);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        printBinary (
                outerPriority, isNegative ? " NOT LIKE " : " LIKE ",
                OpPriority.RELATIONAL,
                InfixAssociation.LEFT,
                s
        );
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (super.equals(obj) && isNegative == ((LikeExpression) obj).isNegative);
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * (isNegative ? 23 : 41));
    }
}