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
package com.epam.deltix.util.jcg.scg;

import com.epam.deltix.util.jcg.*;
import java.io.*;
import java.lang.reflect.*;

/**
 *
 */
class CSMethodImpl extends MethodImpl {
    private boolean         isOverride = false;
    
    CSMethodImpl (ClassImpl container, int modifiers, String type, String name) {
        super (container, modifiers, type, name);
    }

    @Override
    public void             addAnnotation (JAnnotation annotation) {
        if (annotation == CSharpSrcGenContext.OVERRIDE_PSEUDO_ANNOTATION)
            isOverride = true;
        else
            super.addAnnotation (annotation);
    }   

    @Override
    void                    printModifiers (SourceCodePrinter out) 
        throws IOException 
    {
        if (isOverride)
            out.print ("override ");
        
        context.printModifiers (modifiers () & ~Modifier.FINAL, out);            
    }        
}