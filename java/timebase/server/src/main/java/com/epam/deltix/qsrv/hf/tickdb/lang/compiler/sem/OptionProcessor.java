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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledConstant;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.GrammarUtil;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.OptionElement;

/**
 *
 */
public abstract class OptionProcessor <T> {    
    public final String     key;
    public final DataType   valueType;

    protected OptionProcessor (String key, DataType valueType) {
        this.key = key.toUpperCase ();
        this.valueType = valueType;
    }
        
    public abstract void    process (
        OptionElement           option, 
        CompiledConstant        value, 
        T                       target
    );        
    
    protected void          printValue (T source, StringBuilder out) {
        throw new UnsupportedOperationException ();
    }
    
    protected boolean       shouldPrint (T source) {
        return (true);
    }
    
    public final boolean    print (T source, StringBuilder out) {
        if (!shouldPrint (source))
            return (false);
        
        if (valueType != null) {        
            GrammarUtil.escapeVarId (key, out);
            out.append (" = ");
            printValue (source, out);
            return (true);
        }

        GrammarUtil.escapeVarId (key, out);
        return (true);        
    }        
}