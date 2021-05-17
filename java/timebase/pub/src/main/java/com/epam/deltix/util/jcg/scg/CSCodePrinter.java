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
package com.epam.deltix.util.jcg.scg;

/**
 *
 */
public final class CSCodePrinter extends NiceSourceCodePrinter {
    public enum ImportMode {
        EXPLICIT,
        BY_NAMESPACE
    };
    
    private String              namespace = null;
    private String              topComment = null;
    private ImportTracker       importTracker = null;
    
    public CSCodePrinter (String namespace) {        
        this.namespace = namespace;        
    }

    public void                 setImportMode (ImportMode mode) {
        switch (mode) {
            case EXPLICIT:
                importTracker = null;
                break;
                
            case BY_NAMESPACE:
                importTracker = new CSNSImportTracker ();
                break;                
        }
    }

    @Override
    public ImportTracker        getImportTracker () {
        return (importTracker);
    }    
    
    public String               getNamespace () {
        return namespace;
    }

    public String               getTopComment () {
        return topComment;
    }

    public void                 setTopComment (String topComment) {
        this.topComment = topComment;
    }

    
    @Override
    protected void              doPrintHeader (StringBuilder out) {
        if (importTracker != null) 
            importTracker.printImports (namespace, out);
        
        if (namespace != null) {
            out.append ("namespace ");
            out.append (namespace);
            out.append (" {\n");
        }  
        
        if (topComment != null) {
            out.append ("    /// <summary>");
            
            for (String s : topComment.split ("\n")) {
                out.append ("\n    /// ");
                out.append (s);
            }
            
            out.append ("    /// </summary>\n");
        }                
    }    
    
    @Override
    protected void              doPrintFooter (StringBuilder out) {
        if (namespace != null)
            out.append ("}");
    }
   
    
}
