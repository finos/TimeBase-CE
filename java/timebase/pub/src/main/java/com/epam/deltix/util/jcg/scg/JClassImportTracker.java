/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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