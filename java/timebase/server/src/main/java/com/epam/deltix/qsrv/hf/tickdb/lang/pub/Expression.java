package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Element;

/**
 *
 */
public abstract class Expression extends Element {
    protected Expression (long location) {
        super (location);
    }

    public final void           print (StringBuilder s) {
        print (OpPriority.OPEN, s);
    }

    protected abstract void     print (int outerPriority, StringBuilder s);
}
