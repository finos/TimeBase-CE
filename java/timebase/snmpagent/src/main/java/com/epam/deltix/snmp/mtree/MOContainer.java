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
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import java.util.*;

/**
 * 
 */
public abstract class MOContainer 
    extends MONode <SMIPrimitiveContainer>     
{    
    protected final IntegerToObjectHashMap <MONode <?>>    children =
        new IntegerToObjectHashMap <> ();
    
    protected MOContainer (
        MONode <?>              parent, 
        SMIPrimitiveContainer   prototype
    )
    {
        super (parent, prototype);        
    }
    
    protected MOContainer (
        MOTable                 parent, 
        SMIPrimitiveContainer   prototype,
        SMIOID                  index
    )
    {
        super (parent, prototype, index);                
    }

    protected final void setUpScalarChildren () {
        MONode <?>             child;
        
        for (SMINode childPrototype : getProto ().children ()) {
            if (childPrototype instanceof SMIPrimitive)        
                child = new MOPrimitive (this, (SMIPrimitive) childPrototype);        
            else if (childPrototype instanceof SMICategory)
                child = new MOCategory (this, (SMICategory) childPrototype);
            else if (childPrototype instanceof SMITable)
                child = new MOTable (this, (SMITable) childPrototype);
            else
                throw new UnsupportedOperationException (childPrototype.toString ());
        
            children.put (childPrototype.getOid ().getLast (), child);
        }
    }
    
    @Override
    public void         clearCache () {
        Enumeration <MONode <?>>    e = children.elements ();
        
        while (e.hasMoreElements ())
            e.nextElement ().clearCache ();
    }
    
    @Override
    public void         setCacheValid () {
        Enumeration <MONode <?>>    e = children.elements ();
        
        while (e.hasMoreElements ())
            e.nextElement ().setCacheValid ();
    }
    
    public void         walk () throws Exception {
        clearCache ();
        
        getRoot ().getSPI ().walk (getProto ().getOid (), this);
        
        setCacheValid ();
    }
    
    public MONode <?>   getNodeByName (String name) {
        return (getRoot ().getNodeByName (name));
    }

    public MONode <?>   getDirectChildById (int id) {
        return (children.get (id, null));    
    }
    
    public Integer []   getDirectChildIds () {
        return (getProto ().getChildIds ());
    }
    
    @Override
    public void         set (SMIOID oid, Object value) {
        SMIOID              myOid = getOid ();
        
        if (!oid.startsWith (myOid))
            throw new IllegalArgumentException (oid + " not under " + this);
            
        int                 myOidLength = myOid.getLength ();
        
        if (oid.getLength () == myOidLength)
            throw new IllegalArgumentException (
                this + " cannot have a value (" + value + ")"
            );
        
        int                 childId = oid.getId (myOidLength);        
        MONode <?>          child = children.get (childId, null);
        
        if (child == null)
            throw new IllegalArgumentException (
                this + " does not have a child at " + childId + 
                "; cannot set (oid=" + oid + "; value=" + value + ")"
            );
        
        child.set (oid, value);
    }
}