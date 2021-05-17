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
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler;
import java.util.ArrayList;

/**
 *
 */
public class OverloadedFunctionSet {
    public final String                             id;
    private final ArrayList <FunctionDescriptor>    descriptors =
        new ArrayList <FunctionDescriptor> ();
    private final ArrayList <DataType []>           argMap =
        new ArrayList <DataType []> ();

    public OverloadedFunctionSet (String id) {
        this.id = id;
    }        
    
    public void         add (FunctionDescriptor fd) {
        descriptors.add (fd);
        
        String []               args = fd.info.args ();
        int                     numArgs = args.length;
        
        while (argMap.size () <= numArgs)
            argMap.add (null);
        
        DataType []    union = argMap.get (numArgs);
        
        if (union == null) 
            argMap.set (numArgs, fd.signature.clone ());        
        else {
            for (int ii = 0; ii < numArgs; ii++) 
                union [ii] = QQLCompiler.unionEx (union [ii], fd.signature [ii]);
        }            
    }
    
    public DataType []      getSignature (int numArgs) {
        if (argMap.size () <= numArgs)
            return (null);
        
        return (argMap.get (numArgs));
    }
    
    public FunctionDescriptor   getDescriptor (DataType [] argTypes) {
        FunctionDescriptor          ret = null;
        
        for (FunctionDescriptor fd : descriptors) {
            if (fd.accept (argTypes)) {
                if (ret == null)
                    ret = fd;
                else
                    return (null);
            }                
        }
        
        return (ret);
    }
}
