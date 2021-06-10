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
package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.mibc.errors.*;
import com.epam.deltix.util.parsers.*;

import java.util.*;

/**
 *
 */
public class ModuleRegistry {
    private Map <String, CompiledModule>        env = 
        new HashMap <String, CompiledModule> ();    
    
    ModuleRegistry () {
    }
    
    public static ModuleRegistry    create () {
        ModuleRegistry      mreg = new ModuleRegistry ();

        mreg.register (StandardModules.SNMPv2_SMI);
        mreg.register (StandardModules.SNMPv2_TC);
        mreg.register (StandardModules.SNMPv2_CONF);
        mreg.register (StandardModules.SNMPv2_MIB);
        mreg.register (StandardModules.IANAifType_MIB);
        mreg.register (StandardModules.IF_MIB);

        return (mreg);
    }
    
    public final void               register (CompiledModule module) {
        String          id = module.getId ();
        
        if (env.containsKey (id))
            throw new DuplicateIdException (Location.NONE, id);
        
        env.put (id, module);
    }
    
    public final CompiledModule     getModule (String id) {
        return (getModule (Location.NONE, id));
    }
    
    public final CompiledModule     getModule (long refLocation, String id) {
        if (!Character.isUpperCase (id.charAt (0)))
            throw new IllegalArgumentException (id);
        
        CompiledModule              cmod = env.get (id);
        
        if (cmod == null)
            throw new UnresolvedIdException (refLocation, id);
        
        return (cmod);
    }
}
