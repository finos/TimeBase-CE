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
package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

import java.util.Hashtable;

/**
 *
 */
public class NonStaticAttributeDef extends AttributeDef {
    public final DataTypeSpec       type;
    public final Identifier         relativeId;
    public final Expression         defval;
    
    public NonStaticAttributeDef (
        long                    location, 
        String                  id,
        String                  title,
        String                  comment, 
        DataTypeSpec            type,
        Identifier              relativeId,
        Expression              defval,
        Hashtable<String, String> tags
    )
    {
        super (id, title, tags, comment, location);
        this.type = type;
        this.relativeId = relativeId;
        this.defval = defval;
    }

    public NonStaticAttributeDef (
        String                  id,
        String                  title,
        String                  comment, 
        DataTypeSpec            type,
        Identifier              relativeId,
        Expression              defval,
        Hashtable<String, String> tags
    )
    {
        this (Location.NONE, id, title, comment, type, relativeId, defval, tags);
    }

    @Override
    public void print (StringBuilder s) {
        printHeader (s);
        s.append (' ');
        type.print (s);
        
        if (relativeId != null) {
            s.append (" RELATIVE TO ");
            relativeId.print (s);
        }
        
        if (defval != null) {
            s.append (" DEFAULT ");
            defval.print (s);
        }

        printTags(s);
        printComment (s);
    }        
}