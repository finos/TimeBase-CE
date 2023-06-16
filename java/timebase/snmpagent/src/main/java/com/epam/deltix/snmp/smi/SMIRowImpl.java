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
public class SMIRowImpl
    extends SMIPrimitiveContainerImpl <SMITableImpl> 
    implements SMIRow 
{    
    private final SMIIndexInfo        indexInfo;
    
    /**
     *  Row with primary index
     */
    public SMIRowImpl(
            SMITableImpl parent,
            SMIOID oid,
            String name,
            String description,
            int numIndexedChildren,
            boolean lastIndexImplicit
    )
    {
        super (parent, oid, name, description);        
        
        if (numIndexedChildren < 1)
            throw new IllegalArgumentException ("No indexes defined");
        
        indexInfo = new SMIPrimaryIndexImpl (numIndexedChildren, lastIndexImplicit);
    }

    /**
     *  Augmenting row
     */
    SMIRowImpl (
        SMITableImpl            parent, 
        SMIOID                  oid, 
        String                  name,
        String                  description,
        SMIRow                  augmentsRow
    )
    {
        super (parent, oid, name, description);        
        
        indexInfo = new SMIAugmentedIndexImpl (augmentsRow);
    }

    @Override
    public SMIIndexInfo         getIndexInfo () {
        return (indexInfo);
    }
        
    @Override
    public SMIPrimitive         addObjectType (
        int                         id, 
        String                      name, 
        SMIType                     type, 
        SMIAccess                   access,
        String                      description,
        int                         indexDepth
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
        
        if (indexDepth >= 0) {
            if (!(indexInfo instanceof SMIPrimaryIndexImpl))
                throw new IllegalArgumentException (
                    this + " was not constructed with the primary index flags"
                );
            
            SMIPrimaryIndexImpl     pi = (SMIPrimaryIndexImpl) indexInfo;
            SMIPrimitive []         indexes = pi.indexedChildren;
            
            if (indexDepth >= indexes.length)
                throw new IllegalArgumentException (
                    "Index depth " + indexDepth + " > max " + indexes.length
                );
            
            if (indexes [indexDepth] != null)
                throw new IllegalArgumentException(
                        String.format("Index at depth %d is already set: %s [%s] clashes with %s [%s]", indexDepth,
                                indexes[indexDepth], indexes[indexDepth].getDescription(),
                                node, node.getDescription())
                );
            
            indexes [indexDepth] = node;
        }            
        
        return (node);
    }
}