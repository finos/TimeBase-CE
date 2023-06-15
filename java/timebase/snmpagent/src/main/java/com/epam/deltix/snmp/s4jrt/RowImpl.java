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
package com.epam.deltix.snmp.s4jrt;

import org.snmp4j.agent.mo.*;
import org.snmp4j.smi.*;

/**
 *
 */
public final class RowImpl <EntryType> implements MOTableRow {
    final EntrySupport <EntryType>              support;
    final OID                                   index;
    final EntryType                             entry;
    
    public RowImpl (
        EntrySupport <EntryType>                support,
        EntryType                               entry
    )
    {
        this.support = support;
        this.index = support.getIndex (entry);
        this.entry = entry;
    }        

    @Override
    public MOTableRow               getBaseRow () {
        return (null);
    }

    @Override
    public OID                      getIndex () {
        return (index);
    }

    @Override
    public Variable                 getValue (int column) {
        return (support.getValue (entry, column));
    }

    @Override
    public void                     setBaseRow (MOTableRow baseRow) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    @Override
    public int                      size () {   
        return (support.getNumColumns ());
    }
}