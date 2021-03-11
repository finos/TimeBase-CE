package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JMember {
    public String       name ();

    public int          modifiers ();

    public JClass       containerClass ();
}
