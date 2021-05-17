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

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.*;
import com.epam.deltix.util.lang.*;

/**
 *
 */
public class PredicateCompiler {
    private final StdEnvironment            stdEnv = new StdEnvironment (null);
    private final RecordClassDescriptor []  inputTypes;
    
    public PredicateCompiler (RecordClassDescriptor ... inputTypes) {    
        this.inputTypes = inputTypes;
    }
    
    public MessagePredicate   compile (Expression e) {
        QQLExpressionCompiler   ecomp = 
            new QQLExpressionCompiler (stdEnv);
        
        ecomp.setUpClassSetEnv (inputTypes);
        
        CompiledExpression      ce = 
            ecomp.compile (e, StandardTypes.NULLABLE_BOOLEAN);
        
        PredicateGenerator      pg = new PredicateGenerator (inputTypes, ce);
        
        JavaCompilerHelper      helper = 
            new JavaCompilerHelper (MessagePredicate.class.getClassLoader ());
        
        return (pg.finish (helper));        
    }
}
