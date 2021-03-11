package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *  Less or greater, possibly equals.
 */
public enum OrderRelation {
    LT,
    LE,
    GT,
    GE;
    
    public OrderRelation    flip () {
        switch (this) {
            case LT:    return (GT);
            case LE:    return (GE);
            case GT:    return (LT);
            case GE:    return (LE);
            default:    throw new RuntimeException ();
        }
    }
};
