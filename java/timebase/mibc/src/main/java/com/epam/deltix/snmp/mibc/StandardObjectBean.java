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

import com.epam.deltix.snmp.smi.*;

/**
 *
 */
public abstract class StandardObjectBean 
    extends StandardEntityBean 
    implements CompiledObject
{
    private final SMIOID        oid;
    private final SMIType       type;
    
    protected StandardObjectBean (String id, SMIOID oid, SMIType type) {
        super (id);
        
        this.oid = oid;
        this.type = type;
    }

    @Override
    public SMIOID               getOid () {
        return (oid);
    }  
    
    @Override
    public SMIType              getType () {
        return (type);
    }
    
    @Override
    public SMIAccess            getAccess () {
        return (null);
    }
    
    @Override
    public String               getDescription () {
        return (null);
    }

    @Override
    public CompiledIndexInfo    getIndexInfo () {
        return (null);
    }        
}
