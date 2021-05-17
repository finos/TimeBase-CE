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

import java.util.Iterator;

/**
 *
 */
public final class RowToEntryIterator <EntryType> 
    implements Iterator <EntryType> 
{
    private final Iterator <RowImpl <EntryType>> rowIter;

    public RowToEntryIterator (Iterator <RowImpl <EntryType>> rowIter) {
        this.rowIter = rowIter;
    }

    @Override
    public boolean          hasNext () {
        return (rowIter.hasNext ());
    }

    @Override
    public EntryType        next () {
        return (rowIter.next ().entry);
    }

    @Override
    public void             remove () {
        rowIter.remove ();
    }        
}
