package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public final class Null extends Constant {
    public Null (long location) {
        super (location);
    }

    public Null () {
        this (NO_LOCATION);
    }

    protected void      print (int outerPriority, StringBuilder s) {
        s.append ("null");
    }
}
