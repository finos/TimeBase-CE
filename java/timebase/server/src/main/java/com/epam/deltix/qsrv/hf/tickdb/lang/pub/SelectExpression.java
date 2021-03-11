package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import java.util.Arrays;

/**
 *
 */
public final class SelectExpression extends ComplexExpression {
    public static final int             MODE_DISTINCT =     1;
    public static final int             MODE_RUNNING =      1 << 1;
    
    private final int                   mode;
    public final FieldIdentifier []     groupBy;
    
    private static Expression []    cat (Expression source, Expression filter, Expression ... selectors) {
        int             n = selectors == null ? 0 : selectors.length;
        Expression []   a = new Expression [n + 2];

        a [0] = source;
        a [1] = filter;

        if (n != 0)
            System.arraycopy (selectors, 0, a, 2, n);

        return (a);
    }

    public SelectExpression (
        long                    location,
        Expression              source,
        Expression              filter,
        int                     mode,
        FieldIdentifier []      groupBy,
        Expression ...          selectors
    )
    {
        super (location, cat (source, filter, selectors));
        this.mode = mode;
        this.groupBy = groupBy;
    }

    public SelectExpression (
        Expression              source,
        Expression              filter,
        int                     mode,
        FieldIdentifier []      groupBy,
        Expression ...          selectors
    )
    {
        this (NO_LOCATION, source, filter, mode, groupBy, selectors);
    }

    public boolean              isDistinct () {
        return ((mode & MODE_DISTINCT) != 0);
    }
    
    public boolean              isRunning () {
        return ((mode & MODE_RUNNING) != 0);
    }
    
    public boolean              isSelectAll () {
        return (args.length == 2);
    }

    public Expression []        getSelectors () {
        int             n = args.length - 2;

        Expression []   a = new Expression [n];

        System.arraycopy (args, 2, a, 0, n);

        return (a);
    }

    public Expression           getSource () {
        return (args [0]);
    }

    public Expression           getFilter () {
        return (args [1]);
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        s.append ("SELECT ");
        
        if (isDistinct ())
            s.append ("DISTINCT ");
        
        if (isRunning ())
            s.append ("RUNNING ");
        
        printCommaSepArgs (2, args.length, s);
        
        s.append (" FROM ");
        getSource ().print (OpPriority.QUERY, s);

        if (getFilter () != null) {
            s.append (" WHERE ");
            getFilter ().print (OpPriority.QUERY, s);
        }
        
        if (groupBy != null) {
            s.append (" GROUP BY ");
            groupBy [0].print (s);
            
            for (int ii = 1; ii < groupBy.length; ii++) {
                s.append (", ");
                groupBy [ii].print (s);
            } 
        }
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            mode == ((SelectExpression) obj).mode &&
            Arrays.equals (groupBy, ((SelectExpression) obj).groupBy)
        );
    }

    @Override
    public int                      hashCode () {
        return ((super.hashCode () * 41 + mode) * 31 + Arrays.hashCode (groupBy));
    }
}
