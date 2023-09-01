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
package com.epam.deltix.snmp.mtree;

import com.epam.deltix.snmp.smi.*;

/**
 * 
 */
public final class MOPrimitive extends MONode <SMIPrimitive> {
    private boolean                     isValid = false;
    private Object                      value = null;    
    
    public MOPrimitive (
        MOContainer             parent, 
        SMIPrimitive            prototype
    ) 
    {
        super (parent, prototype);                
    }

    public MOPrimitive (
        MOContainer             parent, 
        SMIPrimitive            prototype,
        SMIOID                  index
    ) 
    {
        super (parent, prototype, index);                
    }

    @Override
    public void         clearCache () {
        isValid = false;
    }
    
    @Override
    public void         setCacheValid () {
        this.isValid = true;
    }
    
    public void         setValue (Object value) {
        this.value = value;
        this.isValid = true;
    }
    
    @Override
    public Object       getValue () {
        if (!isValid) {
            value = getRoot ().getSPI ().get (getProto ().getOid (), getIndex ());
            isValid = true;
        }
        
        return (value);
    }            
    
    @Override
    public void         set (SMIOID oid, Object value) {
        // for now accept without a precise oid match requirement. 
        // need to figure out the pattern with .0        
        SMIOID              myOid = getOid ();
        
        if (!oid.startsWith (myOid))
            throw new IllegalArgumentException (oid + " not under " + this);
        
        setValue (value);
    }

    @Override
    public String toString () {
        return (super.toString () + " = " + (isValid ? value : "INVALID"));
    }
    
    
}