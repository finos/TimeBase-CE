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
public abstract class MONode <T extends SMINode> implements SNMPSPI.Handler {
    private final MONode <?>                     parent;
    private final T                              prototype;
    private final SMIOID                         index;
    
    protected MONode (
        MONode <?>          parent, 
        T                   prototype
    )
    {
        this.parent = parent; 
        this.prototype = prototype;
        this.index = null;    
        
        getRoot ().registerName (this);
    }

    protected MONode (
        MONode <?>          parent, 
        T                   prototype,
        SMIOID              index
    )
    {
        this.parent = parent; 
        this.prototype = prototype;
        this.index = index;            
    }

    public abstract void    clearCache ();
    
    public abstract void    setCacheValid ();
    
    public MORoot           getRoot () {
        return (getParent ().getRoot ());
    }

    public T                getProto () {
        return prototype;
    }
    
    public final SMIOID     getOid () {
        return (prototype.getOid ());
    }
    
    public MONode <?>       getParent () {
        return parent;
    }
    
    public SMIOID           getIndex () {
        return index;
    }

    public Object           getValue () {
        return (this);
    }        
        
    @Override
    public String           toString () {
        return (prototype + " [" + index + "]");
    }        
}