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

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */  
public class FunctionDescriptor {
    public final FunctionInfo               info;
    public final Class <?>                  cls;
    public final DataType []                signature;
    public final DataType                   returnType;
    public final boolean                    aggregate;
    
    public FunctionDescriptor (Class<?> cls) {
        this.cls = cls;
        
        info = cls.getAnnotation (FunctionInfo.class);
        aggregate = cls.isAnnotationPresent (Aggregate.class);
        
        if (info == null)
            throw new IllegalArgumentException (cls + " is missing @FunctionInfo");
        
        String []               args = info.args ();
        int                     numArgs = args.length;
        
        signature = new DataType [numArgs];
        
        for (int ii = 0; ii < numArgs; ii++) {
            String      argtype = args [ii].toUpperCase ();
            DataType    dt = StandardTypes.forName (argtype);

            if (dt == null)
                throw new IllegalArgumentException ("Unrecognized type: " + argtype);

            signature [ii] = dt;
        }     
        
        String      retTypeName = info.returns ();
        
        returnType = StandardTypes.forName (retTypeName);
        
        if (returnType == null)
            throw new IllegalArgumentException ("Unrecognized return type: " + retTypeName);
    }  
    
    public boolean          accept (DataType [] actualArgTypes) {
        int                     numArgs = signature.length;
        
        if (actualArgTypes.length != numArgs)
            return (false);
        
        for (int ii = 0; ii < numArgs; ii++)
            if (signature [ii] != null &&
                !QQLCompiler.isCompatibleWithoutConversion (actualArgTypes [ii], signature [ii]))
                return (false);
        
        return (true);
    }
}
