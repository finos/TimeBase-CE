package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public class NullCheckExpression extends ComplexExpression {
    public final boolean            checkIsNull;

    public NullCheckExpression (long location, Expression arg, boolean checkIsNull) {
        super (location, arg);
        this.checkIsNull = checkIsNull;
    }

    public NullCheckExpression (Expression arg, boolean checkIsNull) {
        super (NO_LOCATION, arg);
        this.checkIsNull = checkIsNull;
    }

    @Override
    protected void              print (int outerPriority, StringBuilder s) {
        getArgument ().print (outerPriority, s);
        s.append (" IS ");

        if (!checkIsNull)
            s.append ("NOT ");

        s.append ("NULL");
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            checkIsNull == ((NullCheckExpression) obj).checkIsNull
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * (checkIsNull ? 23 : 41));
    }
}
