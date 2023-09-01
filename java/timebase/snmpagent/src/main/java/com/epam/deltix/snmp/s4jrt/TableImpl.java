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
package com.epam.deltix.snmp.s4jrt;

import com.epam.deltix.snmp.pub.*;
import java.util.Iterator;
import org.snmp4j.agent.mo.*;

/**
 *
 */
public class TableImpl <EntryType> implements Table <EntryType> {
    private final EntrySupport <EntryType>                          support;
    public final DefaultMOMutableTableModel <RowImpl <EntryType>>   model
        = new DefaultMOMutableTableModel <RowImpl <EntryType>> ();
    
    public TableImpl (EntrySupport <EntryType> support) {
        this.support = support;
    }
    //
    //  IMPLEMENT Table <EntryType>
    //
    @Override
    public void                     add (EntryType entry) {
        model.addRow (new RowImpl <EntryType> (support, entry));
    }

    @Override
    public void                     remove (EntryType obj) {
        model.removeRow (support.getIndex (obj));
    }

    @Override
    public int                      size () {
        return (model.getRowCount ());
    }

    @Override
    public Iterator <EntryType>     iterator () {
        return (new RowToEntryIterator <EntryType> (model.iterator ()));
    }        
}