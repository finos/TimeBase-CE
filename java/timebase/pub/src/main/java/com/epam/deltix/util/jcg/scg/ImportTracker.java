package com.epam.deltix.util.jcg.scg;

/**
 *
 */
public interface ImportTracker {
    public String               getPrintClassName (String name);
    
    public void                 printImports (String pack, StringBuilder out);
}
