package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class BooleanConstant extends Constant {
    public final boolean            value;

    public BooleanConstant (long location, boolean value) {
        super (location);
        this.value = value;
    }

    public BooleanConstant (boolean value) {
        this (NO_LOCATION, value);
    }

    protected void      print (int outerPriority, StringBuilder s) {
        s.append (value);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            value == ((BooleanConstant) obj).value
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * (value ? 23 : 41));
    }
}
