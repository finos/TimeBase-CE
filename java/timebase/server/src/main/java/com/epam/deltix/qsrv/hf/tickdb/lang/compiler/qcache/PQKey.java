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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.qcache;

import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import java.util.Arrays;

/**
 *
 */
class PQKey {
    public final ParamSignature []      paramSignature;
    public final Element                select;

    public PQKey (Element select, ParamSignature [] paramSignature) {
        this.select = select;
        this.paramSignature = paramSignature;        
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean      equals (Object obj) {
        final PQKey other = (PQKey) obj;
        
        if (!Arrays.equals (this.paramSignature, other.paramSignature))
            return false;
        
        if (!this.select.equals (other.select)) 
            return false;
        
        return true;
    }

    @Override
    public int          hashCode () {
        return (4 * Arrays.hashCode (paramSignature) + select.hashCode ());
    }           
}

