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

import java.io.*;
import java.util.Arrays;

/**
 *
 */
public abstract class NiceSourceCodePrinter extends SourceCodePrinter {
    private boolean                     finished = false;
        
    public NiceSourceCodePrinter () {
        super (new StringBuilder ());        
    }

    public abstract ImportTracker   getImportTracker ();
    
    protected abstract void         doPrintHeader (StringBuilder out);
    
    protected void                  doPrintFooter (StringBuilder out) {        
    }
    
    @Override
    public void                     finish () {  
        if (finished)
            return;
        
        finished = true;
        
        try {
            super.finish ();
        } catch (IOException x) {
            throw new RuntimeException (x);
        }
        
        StringBuilder   head = new StringBuilder ();

        doPrintHeader (head);

        ((StringBuilder) out).insert (0, head);     

        doPrintFooter ((StringBuilder) out);        
    }
    
    public String                   getSourceCode () {
        finish ();        
        return (out.toString ());
    }
    
    @Override
    public void                     printRefClassName (String cn) 
        throws IOException 
    {       
        super.print (
            getImportTracker () == null ? 
                cn : 
                getImportTracker ().getPrintClassName (cn)
        );        
    }        

    public void                     printRefClassName (String className, String[] typeArgs) throws IOException {
        super.printRefClassName(
                getImportTracker() == null ? className : getImportTracker().getPrintClassName(className),
                Arrays.stream(typeArgs).map(arg -> getImportTracker() == null ? arg: getImportTracker().getPrintClassName(arg))
                        .toArray(String[]::new)
        );
    }
}