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

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.*;

/**
 *  Base class for compiled filters.
 */
public abstract class FilterBase implements PreparedQuery {
    public PreparedQuery                sourceQuery;
    public RecordClassDescriptor []     types;
    public RecordClassDescriptor []     inputTypes;
    public RecordClassDescriptor []     outputTypes;

    public boolean                      isReverse () {
        return (sourceQuery.isReverse ());
    }
    
    public void                         close () {
        sourceQuery.close ();
        sourceQuery = null;
        types = null;
    }
}
