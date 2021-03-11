package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JCompoundStatement
    extends JStatement, JVariableContainer
{
    public void             add (JStatement stmt);

    public void             addFront (JStatement stmt);

    public void             add (JExpr e);

    public void             addComment (String text);

    public JLocalVariable   addVar (
        int                     modifiers,
        JType                   type,
        String                  name
    );

    public JLocalVariable   addVar (
        int                     modifiers,
        JType                   type,
        String                  name,
        JExpr                   initValue
    );

    public JLocalVariable   addVar (
        int                     modifiers,
        Class <?>               type,
        String                  name
    );

    public JLocalVariable   addVar (
        int                     modifiers,
        Class <?>               type,
        String                  name,
        JExpr                   initValue
    );
}
