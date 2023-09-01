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

/**
 *
 */
abstract class SMINodeImpl <P extends SMIComplexNode> implements SMINode {
    private final P                     parent;
    private final String                name;
    private final SMIOID                oid;
    private final String                description;
    
    protected SMINodeImpl (
        P                   parent,
        SMIOID              oid,
        String              name,
        String              description
    )
    {
        this.parent = parent;
        this.name = name;
        this.oid = oid;   
        this.description = description;
    }

    @Override
    public int              getId () {
        return (oid.getLast ());
    }
    
    @Override
    public P                getParent () {
        return parent;
    }
    
    @Override
    public final SMIOID     getOid () {
        return oid;
    }

    @Override
    public final String     getName () {
        return name;
    }

    @Override
    public final String     getDescription () {
        return description;
    }

    @Override
    public String           toString () {
        return ("node at " + oid);
    }
}