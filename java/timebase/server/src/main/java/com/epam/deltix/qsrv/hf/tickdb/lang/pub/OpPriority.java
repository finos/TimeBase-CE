package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *
 */
public abstract class OpPriority {
    public static final int                 OPEN = 0;

    public static final int                 UNION = 1;
    public static final int                 QUERY = 2;
    public static final int                 COMMA = 3;
    public static final int                 LOGICAL_OR = 4;
    public static final int                 LOGICAL_AND = 5;
    public static final int                 RELATIONAL = 6;
    public static final int                 ADDITION = 7;
    public static final int                 MULTIPLICATION = 8;
    public static final int                 PREFIX = 9;
    public static final int                 POSTFIX = 10;
}
