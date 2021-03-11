package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class BetweenExpression extends ComplexExpression {
    public BetweenExpression (long location, Expression arg, Expression min, Expression max) {
        super (location, arg, min, max);
    }

    public BetweenExpression (OrderRelation relation, Expression arg, Expression min, Expression max) {
        this (NO_LOCATION, arg, min, max);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        boolean                     parenthesize = outerPriority > OpPriority.LOGICAL_AND;

        if (parenthesize)
            s.append ("(");

        int                         p = OpPriority.LOGICAL_AND + 1;
        
        args [0].print (p, s);
        s.append (" BETWEEN ");
        args [1].print (p, s);
        s.append (" AND ");
        args [2].print (p, s);

        if (parenthesize)
            s.append (")");
    }
}
