package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *  The "*" in "select * ..."
 */
public final class This extends Expression {
    public This (long location) {
        super (location);
    }

    public This () {
        this (NO_LOCATION);
    }

    protected void      print (int outerPriority, StringBuilder s) {
        s.append ("*");
    }
}
