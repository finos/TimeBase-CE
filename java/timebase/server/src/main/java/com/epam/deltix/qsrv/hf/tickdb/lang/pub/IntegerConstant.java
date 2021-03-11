package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.lang.Util;

/**
 *
 */
public final class IntegerConstant extends Constant {
    public final long            value;

    public IntegerConstant (long location, long value) {
        super (location);
        this.value = value;
    }

    protected void      print (int outerPriority, StringBuilder s) {
        s.append (value);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            value == ((IntegerConstant) obj).value
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + Util.hashCode (value));
    }
}
