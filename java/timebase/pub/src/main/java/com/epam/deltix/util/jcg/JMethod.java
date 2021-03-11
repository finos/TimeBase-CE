package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JMethod extends JCallable, JAnnotationContainer{
    public JExpr                staticCall (JExpr ... args);

    public JExpr                call (JExpr obj, JExpr ... args);

    public JExpr                callThis (JExpr ... args);

    public void addException(Class<? extends Throwable> throwable);
}
