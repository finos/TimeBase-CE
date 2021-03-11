package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class UnaryMinusExpression extends ComplexExpression {
    public UnaryMinusExpression (long location, Expression arg) {
        super (location, arg);
    }

    public UnaryMinusExpression (Expression arg) {
        this (NO_LOCATION, arg);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        printPrefix (outerPriority, "-", OpPriority.ADDITION, s);
    }
}
