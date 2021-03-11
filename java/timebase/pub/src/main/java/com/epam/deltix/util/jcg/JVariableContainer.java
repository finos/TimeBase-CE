package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JVariableContainer {
    public void             addComment (String text);

    public JInitVariable    addVar (
        int                     modifiers,
        JType                   type,
        String                  name
    );

    public JInitVariable    addVar (
        int                     modifiers,
        JType                   type,
        String                  name,
        JExpr                   initValue
    );

    public JInitVariable    addVar (
        int                     modifiers,
        Class <?>               type,
        String                  name
    );

    public JInitVariable    addVar (
        int                     modifiers,
        Class <?>               type,
        String                  name,
        JExpr                   initValue
    );
}
