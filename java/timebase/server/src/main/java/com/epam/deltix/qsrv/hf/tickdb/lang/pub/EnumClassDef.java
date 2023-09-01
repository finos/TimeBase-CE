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
public final class EnumClassDef extends ClassDef {
    public final boolean            isFlags;
    public final EnumValueDef []    values;

    public EnumClassDef (
        long                location,
        TypeIdentifier      id, 
        String              title, 
        String              comment,
        boolean             isFlags, 
        EnumValueDef ...    values        
    )
    {
        super (location, id, title, null, comment);
        this.isFlags = isFlags;
        this.values = values;
    }
    
    public EnumClassDef (
        TypeIdentifier      id, 
        String              title, 
        String              comment,
        boolean             isFlags, 
        EnumValueDef ...    values        
    )
    {
        this (Location.NONE, id, title, comment, isFlags, values);
    }
    
    @Override
    public void             print (StringBuilder s) {
        s.append ("ENUM ");
        printHeader (s);
        s.append (" (");
        
        boolean     first = true;
        
        for (EnumValueDef vd : values) {
            if (first) {
                first = false;
                s.append ('\n');
            }
            else
                s.append (",\n");
            
            vd.print (s);
        }
        
        s.append ("\n)");
        printComment (s);
    }
}