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
final class SMICategoryImpl
    extends SMIPrimitiveContainerImpl <SMICategoryImpl>
    implements SMICategory 
{
    SMICategoryImpl (
        SMICategoryImpl         parent, 
        SMIOID                  oid, 
        String                  name,
        String                  description
    )
    {
        super (parent, oid, name, description);
    }

    @Override
    public SMICategory          addObjectIdentifier (int id, String name) {
        checkChild (id, name);
        
        SMICategoryImpl      node = 
            new SMICategoryImpl (this, new SMIOID (getOid (), id), name, null);
        
        registerChild (node);
        
        return (node);
    }
    
    @Override
    public SMITable             addTable (
        int                         id,
        String                      name, 
        String                      description
    )
    {
        checkChild (id, name);
        
        SMITableImpl      node = 
            new SMITableImpl (
                this, 
                new SMIOID (getOid (), id), 
                name, 
                description
            );
        
        registerChild (node);
        
        return (node);
    }
    
    @Override
    public SMIPrimitive         addObjectType (
        int                         id, 
        String                      name, 
        SMIType                     type, 
        SMIAccess                   access,
        String                      description
    )
    {
        checkChild (id, name);
        
        SMIPrimitiveNodeImpl      node = 
            new SMIPrimitiveNodeImpl (
                this, 
                new SMIOID (getOid (), id), 
                name,
                type,
                access,
                description
            );
        
        registerChild (node);
        
        return (node);
    }        
}