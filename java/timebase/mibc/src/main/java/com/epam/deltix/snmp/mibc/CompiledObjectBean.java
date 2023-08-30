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

import com.epam.deltix.snmp.parser.*;
import com.epam.deltix.snmp.smi.*;

/**
 *
 */
class CompiledObjectBean 
    extends CompiledEntityBean <ObjectDefinition> 
    implements CompiledObject 
{
    SMIOID                      oid = null;
    SMIType                     type = null;
    CompiledIndexInfo           indexInfo = null;
    
    CompiledObjectBean (ObjectDefinition def) {
        super (def);
    }

    @Override
    public SMIOID               getOid () {
        if (oid == null)
            throw new IllegalStateException ("uncompiled");
        
        return (oid);
    }
    
    @Override
    public SMIType              getType () {
        return (type);
    }
    
    @Override
    public SMIAccess            getAccess () {
        if (def instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition    otd = (ObjectTypeDefinition) def;
            
            return (SMIAccess.valueOf (otd.maxAccess.replace ('-', '_')));
        }
        
        return (null);
    }
    
    @Override
    public String               getDescription () {
        if (def instanceof ObjectTypeDefinition) {
            ObjectTypeDefinition    otd = (ObjectTypeDefinition) def;
            
            return (otd.description);
        }
        
        return (null);
    }

    @Override
    public CompiledIndexInfo    getIndexInfo () {
        return (indexInfo);
    }        
}