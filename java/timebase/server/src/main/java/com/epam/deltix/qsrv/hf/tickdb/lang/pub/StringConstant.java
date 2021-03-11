package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.lang.Util;

/**
 *
 */
public final class StringConstant extends Constant {
    public final String            value;

    public StringConstant (long location, String value) {
        super (location);
        this.value = value;
    }

    public StringConstant (String value) {
        this (NO_LOCATION, value);
    }

    protected void      print (int outerPriority, StringBuilder s) {
        GrammarUtil.escapeStringLiteral (value, s);
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            Util.xequals (value, ((StringConstant) obj).value)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + Util.xhashCode (value));
    }
}
