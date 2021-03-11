package com.epam.deltix.util.jcg;

/**
 *
 */
public interface JVariable extends JAnnotationContainer {
    public int              modifiers ();

    public String           name ();

    public String           type ();
}
