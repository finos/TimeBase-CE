package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class AndExpression extends ComplexExpression {
    public AndExpression (long location, Expression left, Expression right) {
        super (location, left, right);
    }

    public AndExpression (Expression left, Expression right) {
        this (NO_LOCATION, left, right);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        printBinary (outerPriority, " AND ", OpPriority.LOGICAL_AND, InfixAssociation.LEFT, s);
    }
}
