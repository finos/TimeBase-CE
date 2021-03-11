package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.lang.Util;
import java.util.*;

/**
 *
 */
public final class CSNSImportTracker implements ImportTracker {
    private TreeSet <String>    imports = new TreeSet <> ();
    
    public void                 registerPackage (String name) {
        imports.add (name);
    }
        
    public void                 registerClass (String name) {
        registerPackage (Util.getPackage (name));
    }
        
    @Override 
    public String               getPrintClassName (String name) {
        registerClass (name);
        
        if (imports.contains (Util.getPackage (name)))
            return (Util.getSimpleName (name));
        else
            return (name);
    } 
    
    public Iterable <String>    imports () {
        return (imports);
    }
    
    @Override
    public void                 printImports (String namespace, StringBuilder out) {
        boolean hasImports = false;
        
        for (String s : imports) {
            if (s.equals (namespace))
                continue;
            
            hasImports = true;
            out.append ("using ");
            out.append (s);
            out.append (";\n");
        }
        
        if (hasImports)
            out.append ('\n');                
    }
}
