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
package com.epam.deltix.snmp.smi;

import java.util.*;

/**
 *
 */
class SMIPrimitiveContainerImpl <P extends SMIComplexNode>
    extends SMINodeImpl <P>
    implements SMIPrimitiveContainer 
{
    private TreeMap <Integer, SMINode>               idToChildNodeMap = 
        new TreeMap <Integer, SMINode> ();
    
    SMIPrimitiveContainerImpl (
        P                       parent, 
        SMIOID                  oid, 
        String                  name,
        String                  description
    )
    {
        super (parent, oid, name, description);
    }

    @Override
    public Collection <SMINode> children () {
        return (idToChildNodeMap.values ());
    }
        
    protected void              checkChild (int id, String name) {
        SMINode         ex = idToChildNodeMap.get (id);
        
        if (ex != null)
            throw new IllegalStateException (
                ex + " is already registered under the id of " + id
            );                
    }
    
    protected void              registerChild (SMINode child) {
        int             id = child.getOid ().getLast ();        
        SMINode         ex = idToChildNodeMap.get (id);
        
        if (ex == null)
            idToChildNodeMap.put (id, child);
        else if (ex != child)
            throw new IllegalStateException (
                ex + " is already registered under the id of '" +
                id + "'; cannot register " + child
            );                
    }
    
    @Override
    public SMINode          getChildById (int i) {
        return (idToChildNodeMap.get (i));
    }

    @Override
    public Integer []       getChildIds () {
        return (idToChildNodeMap.keySet ().toArray (new Integer [idToChildNodeMap.size ()]));
    }     
    
    @Override
    public int              getIndexLength (SMIOID oid, int start) {
        SMIComplexNode          parent = this;
        int                     ii = start;
        int                     oidLength = oid.getLength ();
        
        while (ii < oidLength) {
            int         childId = oid.getId (ii++);        
            SMINode     child = parent.getChildById (childId);

            if (child == null)
                return (MATCH_ERROR_UNKNOWN_ID);

            if (child instanceof SMIPrimitive)
                return (oidLength - ii);   
            
            if (child instanceof SMITable)
                return (MATCH_ERROR_IS_A_TABLE);
            
            parent = (SMICategory) child;
        }
        
        return (MATCH_ERROR_NON_PRIMITIVE_NODE);
    }
}