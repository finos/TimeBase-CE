package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class OrExpression extends ComplexExpression {
    public OrExpression (long location, Expression left, Expression right) {
        super (location, left, right);
    }

    public OrExpression (Expression left, Expression right) {
        this (NO_LOCATION, left, right);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        printBinary (outerPriority, " OR ", OpPriority.LOGICAL_OR, InfixAssociation.LEFT, s);
    }
}
