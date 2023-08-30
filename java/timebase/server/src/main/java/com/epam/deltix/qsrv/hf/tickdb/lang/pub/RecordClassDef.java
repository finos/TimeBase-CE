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

package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *
 */
public final class RecordClassDef extends ClassDef {
    public final AttributeDef []        attributes;
    public final boolean                auxiliary;
    public final boolean                instantiable;
    public final TypeIdentifier         parent;

    public RecordClassDef (
        long                            location,
        TypeIdentifier                  id,                           
        String                          title,
        String                          guid,
        String                          comment,
        boolean                         auxiliary,
        boolean                         instantiable,
        TypeIdentifier                  parent,
        AttributeDef ...                attributes
    )
    {
        super (location, id, title, guid, comment);
        this.attributes = attributes;
        this.auxiliary = auxiliary;
        this.instantiable = instantiable;
        this.parent = parent;
    }
    
    public RecordClassDef (
        TypeIdentifier                  id,                           
        String                          title,
        String                          guid,
        String                          comment,
        boolean                         auxiliary,
        boolean                         instantiable,
        TypeIdentifier                  parent,
        AttributeDef ...                attributes
    )
    {
        this (
            Location.NONE, id, title, guid, comment,
            auxiliary, instantiable, parent, attributes
        );
    }

    @Override
    public void                 print (StringBuilder s) {
        s.append ("CLASS ");
        printHeader (s);
        
        if (parent != null) {
            s.append (" UNDER ");
            parent.print (s);
        }
        
        s.append (" (");
        
        boolean     first = true;
        
        for (AttributeDef ad : attributes) {
            if (first) {
                first = false;
                s.append ('\n');
            }
            else
                s.append (",\n");
            
            ad.print (s);
        }
        
        s.append ("\n)");

        s.append (auxiliary ? "\nAUXILIARY" : "\nNOT AUXILIARY");
        s.append (instantiable ? "\nINSTANTIABLE" : "\nNOT INSTANTIABLE");
        
        printComment (s);
    }        
}
