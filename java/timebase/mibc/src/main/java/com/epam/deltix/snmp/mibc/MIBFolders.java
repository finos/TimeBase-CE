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
package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.parser.*;
import com.epam.deltix.util.text.ShellPatternCSMatcher;
import java.util.*;
import java.io.*;

/**
 *
 */
public class MIBFolders {
    private final Map <String, File>    moduleIdToFile = new HashMap <> ();
    
    public void                     addFolderToIndex (
        File                            folder, 
        boolean                         recursive,
        String                          mibShellPattern
    )
        throws IOException
    {
        File []     files = folder.listFiles ();
        
        if (files == null)
            return;
        
        for (File f : files) {
            if (f.isDirectory ()) {
                if (recursive)
                    addFolderToIndex (f, true, mibShellPattern);
            }
            else if (ShellPatternCSMatcher.INSTANCE.matches (f.getName (), mibShellPattern))
                addMibFileToIndex (f);
        }
    }
    
    public void                     addMibFileToIndex (File f) 
        throws IOException 
    {
        String                      id = MIBParser.getModuleName (f);
        
        if (id != null) {
            synchronized (moduleIdToFile) {
                moduleIdToFile.put (id, f);
            }
        }
    }
    
    public File                     findModule (String moduleId) {
        synchronized (moduleIdToFile) {
            return (moduleIdToFile.get (moduleId));
        }
    }
}