package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JSwitchStatement extends JCompoundStatement {
    /**
     *  Adds a break statement that specifically breaks this switch.
     */
    public void             addBreak ();

    public void             addCaseLabel (JExpr e);

    public void             addCaseLabel (JExpr e, String comment);

    public void             addDefaultLabel ();
}
