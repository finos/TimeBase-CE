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
package com.epam.deltix.qsrv.hf.tickdb.lang.runtime;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;
import com.epam.deltix.util.collections.ArrayEnumeration;
import com.epam.deltix.util.collections.EmptyEnumeration;
import java.util.Enumeration;

/**
 *  Does not group.
 */
public abstract class FlatFilterIMS extends FilterIMSImpl {
    private FilterState                 state;    
    
    public FlatFilterIMS (
        InstrumentMessageSource         source,
        RecordClassDescriptor []        inputTypes,
        RecordClassDescriptor []        outputTypes,
        ReadableValue []                params
    )
    {
        super (source, inputTypes, outputTypes, params);
    }

    protected final FilterState         getState (RawMessage msg) {
        if (state == null)
            state = newState ();
        
        return (state);
    }
    
    protected Enumeration <FilterState>  getStates () {        
        return (
            state == null ? 
                new EmptyEnumeration <FilterState> () : 
                new ArrayEnumeration <FilterState> (state)
        );
    }        
}
