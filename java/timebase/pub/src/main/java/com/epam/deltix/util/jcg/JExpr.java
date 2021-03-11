package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JExpr {
    public JExpr        cast (Class <?> toClass);

    public JExpr        cast (JType toType);

    public JExpr        index (JExpr index);

    public JExpr        index (int index);

    public JExpr        field (String fieldId);

    public JStatement   asStmt ();

    public JExpr        call (String method, JExpr ... args);

    public JExpr        not ();

    public JExpr        incAndGet ();

    public JExpr        decAndGet ();

    public JExpr        getAndInc ();

    public JExpr        getAndDec ();

    public JStatement   inc ();

    public JStatement   dec ();

    public JStatement   alter (String op, JExpr arg);

    public JStatement   throwStmt ();

    public JStatement   returnStmt ();

    public JSwitchStatement switchStmt ();

    public JSwitchStatement switchStmt (String label);

    public JStatement       assign (JExpr value);

    public JExpr            assignExpr (JExpr value);    
}
