/*
 * Copyright 2021 EPAM Systems, Inc
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

import com.epam.deltix.util.lang.Util;
import java.util.*;

/**
 *
 */
public final class JPackImportTracker implements ImportTracker {
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
    public void                 printImports (String pack, StringBuilder out) {
        boolean hasImports = false;
        
        for (String s : imports) {
            if (s.equals (pack) || s.equals ("java.lang"))
                continue;
            
            hasImports = true;
            out.append ("import ");
            out.append (s);
            out.append (".*;\n");
        }
        
        if (hasImports)
            out.append ('\n');                
    }
}
