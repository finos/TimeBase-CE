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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;

/**
 *  Defines an input parameter
 */
public final class ParamSignature {
    public final String             name;
    public final DataType           type;
    
    public ParamSignature (String name, DataType type) {
        this.name = name;
        this.type = type;        
    }
    
    public ParamSignature (Parameter p) {
        this (p.name, p.type);
    }

    public static ParamSignature []     signatureOf (Parameter [] params) {
        if (params == null)
            return (null);
        
        int                     n = params.length;
        
        if (n == 0)
            return (null);
        
        ParamSignature []       ret = new ParamSignature [n];
        
        for (int ii = 0; ii < n; ii++)
            ret [ii] = new ParamSignature (params [ii]);
        
        return (ret);
    }
    
    @Override
    public String           toString () {
        return (
            GrammarUtil.describe (type, false) + " " + 
            GrammarUtil.escapeIdentifier (NamedObjectType.VARIABLE, name)
        );
    }
    
    @Override
    public boolean          equals (Object obj) {
        if (obj == null) 
            return false;
        
        if (getClass () != obj.getClass ()) 
            return false;
        
        final ParamSignature other = (ParamSignature) obj;
        
        return (
            this.name.equals (other.name) && 
            QQLCompiler.isParamTypeCompatible (this.type, other.type)
        );
    }

    @Override
    public int              hashCode () {
        return (
            name.hashCode () * 73 +
            QQLCompiler.paramTypeCompatibilityHashCode (type)
        );
    }        
}