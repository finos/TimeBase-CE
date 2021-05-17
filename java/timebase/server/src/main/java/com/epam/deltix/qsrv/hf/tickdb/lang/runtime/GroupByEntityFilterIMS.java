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
import com.epam.deltix.util.lang.Util;
import java.util.Enumeration;

/**
 *  Groups states by entity, using the very efficient entity index.
 */
public abstract class GroupByEntityFilterIMS extends FilterIMSImpl {
    private FilterState []              states = new FilterState [256];
    private int                         numStates = 0;
    protected int                       currentEntityIdx;
    protected FilterState               currentState;
    
    public GroupByEntityFilterIMS (
        InstrumentMessageSource         source,
        RecordClassDescriptor []        inputTypes,
        RecordClassDescriptor []        outputTypes,
        ReadableValue []                params
    )
    {
        super (source, inputTypes, outputTypes, params);
    }

    protected final FilterState         getState (RawMessage msg) {
        currentEntityIdx = source.getCurrentEntityIndex ();

        final int           curLength = states.length;

        if (currentEntityIdx >= curLength) {
            int             newLength =
                Util.doubleUntilAtLeast (curLength, currentEntityIdx + 1);

            Object []       old = states;
            states = new FilterState [newLength];
            System.arraycopy (old, 0, states, 0, curLength);

            currentState = states [currentEntityIdx] = newState ();
        }
        else {
            currentState = states [currentEntityIdx];

            if (currentState == null) {
                assert currentEntityIdx == numStates;
                
                currentState = states [currentEntityIdx] = newState ();
                numStates = currentEntityIdx + 1;
            }
        }

        return (currentState);
    }
    
    protected Enumeration <FilterState>  getStates () { 
        return (new ArrayEnumeration <FilterState> (states, 0, numStates));
    }
}
