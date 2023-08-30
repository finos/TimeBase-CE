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
package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.mibc.errors.*;
import com.epam.deltix.util.parsers.Location;
import java.util.*;

/**
 *
 */
class Env <T extends CompiledEntity> {
    private Map <String, T>        env = 
        new HashMap <String, T> ();
    
    public final Collection <T>         values () {
        return (env.values ());
    }
    
    public final void                   register (T e) {
        register (Location.NONE, e);
    }
    
    public final void                   register (long location, T e) {
        String          id = e.getId ();
        
        if (env.containsKey (id))
            throw new DuplicateIdException (location, id);
        
        env.put (id, e);
    }
    
    public final T                      resolve (String id) {
        return (resolve (Location.NONE, id));
    }
    
    public final T                      resolve (long location, String id) {
        T      e = env.get (id);
        
        if (e == null)
            throw new UnresolvedIdException (location, id);
        
        return (e);
    }        
}