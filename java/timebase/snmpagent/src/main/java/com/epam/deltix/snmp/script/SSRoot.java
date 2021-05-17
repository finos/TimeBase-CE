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
package com.epam.deltix.snmp.script;

import com.epam.deltix.snmp.mtree.*;
import java.util.*;
import org.mozilla.javascript.Scriptable;

/**
 *
 */
public final class SSRoot extends SSContainer <MORoot> {
    private final Map <MONode <?>, SSNode <?>>      namedScalars =
        new HashMap <MONode <?>, SSNode <?>> ();
        
    public SSRoot (MORoot mo) {
        super (null, mo);    
        
        namedScalars.put (mo, this);
        
        for (Map.Entry <String, MONode <?>> e : mo.scalars ())
            namedScalars.put (e.getValue (), forceWrap (e.getValue ()));
    }

    SSNode <?>                  forceWrap (MONode <?> mo) {
        if (mo instanceof MOContainer)
            return (new SSContainer <MOContainer> (this, (MOContainer) mo));
        
        if (mo instanceof MOPrimitive)
            return (new SSPrimitive (this, (MOPrimitive) mo));

        if (mo instanceof MOTable)
            return (new SSTable (this, (MOTable) mo));

        throw new UnsupportedOperationException (mo.toString ());
    }
    
    Object                      wrap (MONode <?> mo) {
        if (mo == null)
            return (NOT_FOUND);
        
        if (mo == this.mo)
            return (this);
        
        Object              cached = namedScalars.get (mo);
        
        if (cached != null)
            return (cached);
        
        return (forceWrap (mo));
    }

    public void                     exportAllNames (Scriptable scope) {
        for (Map.Entry <String, MONode <?>> e : mo.scalars ())
            scope.put (e.getKey (), scope, wrap (e.getValue ()));
    }       
}
