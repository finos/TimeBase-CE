package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class NotExpression extends ComplexExpression {
    public NotExpression (long location, Expression arg) {
        super (location, arg);
    }

    public NotExpression (Expression arg) {
        this (NO_LOCATION, arg);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        printPrefix (outerPriority, "NOT ", OpPriority.PREFIX, s);
    }
}
