package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public class InExpression extends ComplexExpression {
    public final boolean        positive;

    private static Expression [] cat (Expression arg, Expression ... tests) {
        Expression []   ret = new Expression [tests.length + 1];

        ret [0] = arg;
        System.arraycopy (tests, 0, ret, 1, tests.length);

        return (ret);
    }

    public InExpression (long location, boolean positive, Expression arg, Expression ... tests) {
        super (location, cat (arg, tests));
        this.positive = positive;
    }

    public InExpression (boolean positive, Expression arg, Expression ... tests) {
        this (NO_LOCATION, positive, arg, tests);
    }

    @Override
    public Expression           getArgument () {
        return (args [0]);
    }

    public Expression           getTest (int idx) {
        return (args [1 + idx]);
    }

    public int                  getNumTests () {
        return (args.length - 1);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        int     n = args.length;
        
        args [0].print (s);

        if (!positive)
            s.append (" NOT");

        s.append (" IN (");
        printCommaSepArgs (1, args.length, s);
        s.append (")");
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            positive == ((InExpression) obj).positive
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * (positive ? 23 : 41));
    }
}
