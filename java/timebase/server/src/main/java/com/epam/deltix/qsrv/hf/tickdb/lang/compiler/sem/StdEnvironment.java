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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.NamedObjectType;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.std.*;

/**
 *
 */
public class StdEnvironment extends EnvironmentFrame {
//    public static final EnumClassDescriptor INSTR_TYPE_ECD;
//    public static final EnumDataType        INSTR_TYPE_ENUM;
//
//    static {
//        Introspector    ix = Introspector.createEmptyMessageIntrospector ();
//
//        try {
//            INSTR_TYPE_ECD = ix.introspectEnumClass (InstrumentType.class);
//        } catch (Introspector.IntrospectionException x) {
//            throw new RuntimeException ("Error introspecting built-in types", x);
//        }
//
//        INSTR_TYPE_ENUM = new EnumDataType (false, INSTR_TYPE_ECD);
//    }
//
    public StdEnvironment (Environment parent) {
        super (parent);
        
        register (StandardTypes.CLEAN_BOOLEAN);
        register (StandardTypes.CLEAN_BINARY);
        register (StandardTypes.CLEAN_CHAR);
        register (StandardTypes.CLEAN_FLOAT);
        register (StandardTypes.CLEAN_INTEGER);
        register (StandardTypes.CLEAN_TIMEOFDAY);
        register (StandardTypes.CLEAN_TIMESTAMP);
        register (StandardTypes.CLEAN_VARCHAR);
                        
//        register (new ClassMap.EnumClassInfo (INSTR_TYPE_ECD));
//        QQLCompiler.setUpEnv (this, INSTR_TYPE_ECD);
        
        registerFunction (MaxBoolean.class);
        registerFunction (MaxInteger.class);
        registerFunction (MaxFloat.class);
        registerFunction (MaxChar.class);
        registerFunction (MaxVarchar.class);
        registerFunction (MaxTimeOfDay.class);
        registerFunction (MaxTimestamp.class);
        
        registerFunction (MinBoolean.class);
        registerFunction (MinInteger.class);
        registerFunction (MinFloat.class);
        registerFunction (MinChar.class);
        registerFunction (MinVarchar.class);
        registerFunction (MinTimeOfDay.class);
        registerFunction (MinTimestamp.class);
        
        registerFunction (Count.class);      
        
        bindPseudoFunction (QQLCompiler.KEYWORD_LAST);
        bindPseudoFunction (QQLCompiler.KEYWORD_FIRST);
        bindPseudoFunction (QQLCompiler.KEYWORD_REVERSE);
        bindPseudoFunction (QQLCompiler.KEYWORD_LIVE);
        bindPseudoFunction (QQLCompiler.KEYWORD_HYBRID);        
    }
    
    private void            bindPseudoFunction (String name) {
        bind (NamedObjectType.FUNCTION, name, name);
    }
    
    public final void       register (DataType type) {
        bind (NamedObjectType.TYPE, type.getBaseName (), type);
    }
    
    public final void       register (ClassMap.ClassInfo <?> ci) {
        bind (NamedObjectType.TYPE, ci.cd.getName (), ci);
    }
    
    public final void       registerFunction (Class <?> cls) {
        FunctionDescriptor      fd = new FunctionDescriptor (cls);
        String                  id = fd.info.id ();
        OverloadedFunctionSet   ofs = (OverloadedFunctionSet) 
            lookUpExactLocal (NamedObjectType.FUNCTION, id);
        
        if (ofs == null) {
            ofs = new OverloadedFunctionSet (id);
            
            bind (NamedObjectType.FUNCTION, id, ofs);
        }
            
        ofs.add (fd);                
    }
}
