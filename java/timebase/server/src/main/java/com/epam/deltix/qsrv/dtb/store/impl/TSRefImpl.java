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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.store.pub.*;

/**
 *
 */
class TSRefImpl implements TSRef {
    private final String                fullPath;
    private final int []                idPath;
    
    TSRefImpl (TSFile tsf) {
        StringBuilder       sb = new StringBuilder (tsf.getName ());
        int                 depth = 1;
        
        for (TSFolderEntry e = tsf.getParent (); ; e = e.getParent ()) {            
            sb.insert (0, '/');
            
            if (e instanceof TSRootFolder) {
                sb.insert (0, e.getPath().getPathString());
                break;
            }
            
            depth++;
            sb.insert (0, e.getName ());                        
        }        
        
        fullPath = sb.toString ();
        
        idPath = new int [depth];
                        
        for (TSFolderEntry e = tsf; !(e instanceof TSRootFolder); e = e.getParent ()) 
            idPath [--depth] = e.getId ();        
        
        assert depth == 0;
    }

    int []                      getIdPath () {
        return (idPath);
    }
    
    @Override
    public String               getPath () {
        return (fullPath);
    }

    @Override
    public String               toString () {
        return "TSRefImpl [" + getPath () + ']';
    }        
}