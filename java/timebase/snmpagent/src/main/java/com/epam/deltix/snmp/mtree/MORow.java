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
package com.epam.deltix.snmp.mtree;

import com.epam.deltix.snmp.smi.*;
import java.util.*;

/**
 *
 */
public class MORow extends MOContainer {
    private final Map <String, MOPrimitive>      nameToNodeMap = 
        new HashMap <String, MOPrimitive> ();
    
    MORow (MOTable table, SMIOID index) {
        super (table, table.getProto ().getConceptualRow (), index);
        
        for (SMINode childPrototype : getProto ().children ()) {
            SMIPrimitive        pcp = (SMIPrimitive) childPrototype;            
            MOPrimitive         child = new MOPrimitive (this, pcp, index);        
            
            children.put (childPrototype.getOid ().getLast (), child);
            
            if (pcp.getName () != null)
                nameToNodeMap.put (pcp.getName (), child);
        }
    } 
    
    @Override
    public MOPrimitive              getNodeByName (String name) {
        return (nameToNodeMap.get (name));
    }            
}
