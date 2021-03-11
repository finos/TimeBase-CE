package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class UnionExpression extends ComplexExpression {
    public UnionExpression (long location, Expression left, Expression right) {
        super (location, left, right);
    }

    public UnionExpression (Expression left, Expression right) {
        this (NO_LOCATION, left, right);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        printBinary (outerPriority, " UNION ", OpPriority.UNION, InfixAssociation.LEFT, s);
    }
}
