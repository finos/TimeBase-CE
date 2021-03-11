package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JConstructor extends JCallable {
    public void        callParent (JExpr ... args);

    public void        call (JExpr ... args);
}
