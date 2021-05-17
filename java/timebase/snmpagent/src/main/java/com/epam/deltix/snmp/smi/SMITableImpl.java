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
package com.epam.deltix.snmp.smi;

/**
 *
 */
public final class SMITableImpl
    extends SMINodeImpl <SMIPrimitiveContainer> 
    implements SMITable 
{
    private static final Integer []     NO_IDS = { };
    
    private SMIRowImpl          entry = null;
    
    public SMITableImpl (
        SMICategory             parent, 
        SMIOID                  oid, 
        String                  name, 
        String                  description
    )
    {
        super (parent, oid, name, description);
    }

    @Override
    public SMIRowImpl           getConceptualRow () {
        return (entry);
    }   
    
    private void                checkRowUnset () 
        throws IllegalArgumentException 
    {
        if (entry != null)
            throw new IllegalArgumentException (
                "Entry already registered: " + entry
            );
    }

    @Override
    public SMIRow               addAugmentingRow (
        int                         id,
        String                      name, 
        String                      description,
        SMIRow                      augmentedRow
    )
    {
        checkRowUnset ();
        
        entry = 
            new SMIRowImpl (
                this, 
                new SMIOID (getOid (), id), 
                name, 
                description,
                augmentedRow
            );
        
        return (entry);
    }    
    
    @Override
    public SMIRow               addIndexedRow (
        int                         id,
        String                      name, 
        String                      description,
        int                         numIndexes,
        boolean                     lastIndexIsImplied
    )
    {
        checkRowUnset ();
        
        entry = 
            new SMIRowImpl (
                this, 
                new SMIOID (getOid (), id), 
                name, 
                description,
                numIndexes, 
                lastIndexIsImplied
            );
        
        return (entry);
    }  
    
    @Override
    public SMINode          getChildById (int id) {
        if (entry != null && id == entry.getOid ().getLast ())
            return (entry);
        
        return (null);
    }

    @Override
    public Integer []       getChildIds () {
        if (entry == null)
            return (NO_IDS);
        
        return (new Integer [] { entry.getOid ().getLast () });
    }
    
    
}
