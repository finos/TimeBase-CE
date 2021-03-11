package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.lang.*;
import java.util.*;

/**
 *
 */
public final class JClassImportTracker implements ImportTracker {
    private Map <String, String>    imports = new HashMap <> ();
    
    @Override 
    public String               getPrintClassName (String name) {
        String      sn = Util.getSimpleName (name);
        String      fn = imports.get (sn);
        
        if (fn == null) {
            imports.put (sn, name);
            return (sn);
        }
        
        if (fn.equals (name))
            return (sn);
        //
        //  Conflict
        //
        return (name);
    } 
    
    @Override
    public void                 printImports (String pack, StringBuilder out) {
        boolean hasImports = false;
        
        for (String s : imports.values ()) {
            String  spack = Util.getPackage (s);
            
            if (spack.equals (pack) || spack.equals ("java.lang"))
                continue;
            
            hasImports = true;
            out.append ("import ");
            out.append (s);
            out.append (";\n");
        }
        
        if (hasImports)
            out.append ('\n');                
    }
}
