package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JMemberVariable extends JMember, JVariable {
    public JExpr        access ();

    public JExpr        access (JExpr obj);
}
