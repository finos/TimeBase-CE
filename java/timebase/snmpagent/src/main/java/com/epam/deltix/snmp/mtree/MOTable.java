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
import java.util.*;

/**
 *
 */
public class MOTable extends MONode <SMITable> {
    private boolean                     isValid = false;
    private TreeMap <SMIOID, MORow>     indexMap = 
        new TreeMap <SMIOID, MORow> ();

    public MOTable (
        MOContainer                 parent, 
        SMITable                    prototype
    )
    {
        super (parent, prototype);
        
        if (parent.getIndex () != null)
            throw new IllegalArgumentException ("Nested table");
    }

    @Override
    public void                     clearCache () {
        isValid = false;
        indexMap.clear ();
    }
    
    @Override
    public void                     setCacheValid () {
        this.isValid = true;
    }
    
    private void                    ensureValid () {
        if (!isValid) {
            getRoot ().getSPI ().walk (getOid (), this);
            isValid = true;
        }
    }
    
    public int                      getNumRows () {
        ensureValid ();
        return (indexMap.size ());
    }
    
    public Iterable <SMIOID>        getIds () {
        ensureValid ();
        return (indexMap.keySet ());
    }
        
    public MORow                    getRow (SMIOID index) {
        ensureValid ();
        return (indexMap.get (index));
    }
    
    private MORow                   getOrAddRow (SMIOID index) {
        MORow       row = indexMap.get (index);
        
        if (row == null) {
            row = new MORow (this, index);

            indexMap.put (index, row);
        }
        
        return (row);
    }
    
    @Override
    public void                     set (SMIOID oid, Object value) {
        SMIOID              myOid = getOid ();
        
        if (!oid.startsWith (myOid))
            throw new IllegalArgumentException (oid + " not under " + this);
            
        int                 myOidLength = myOid.getLength ();
        
        if (oid.getLength () == myOidLength)
            throw new IllegalArgumentException (
                this + " cannot have a value (" + value + ")"
            );
        
        SMIRow              entryProto = getProto ().getConceptualRow ();
        
        if (oid.getId (myOidLength) != entryProto.getId ())
            throw new IllegalArgumentException (
                this + "not an entry id; cannot set (oid=" + oid + "; value=" + value + ")"
            );
        
        int                 indexLength = 
            entryProto.getIndexLength (oid, myOidLength + 1);
        
        if (indexLength <= 0)
            throw new IllegalArgumentException (
                this + ": cannot set (oid=" + oid + "; value=" + value + 
                "); error code=" + indexLength
            );
        
        SMIOID              index = oid.getSuffix (indexLength);        
        MORow               row = getOrAddRow (index);
        
        row.set (oid, value);
        
        // We assume that rows are added during a full walk
        isValid = true; 
    }
}