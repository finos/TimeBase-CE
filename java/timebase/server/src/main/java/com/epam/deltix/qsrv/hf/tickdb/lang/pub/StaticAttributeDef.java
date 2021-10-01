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
public final class StaticAttributeDef extends AttributeDef {
    public final DataTypeSpec       type;
    public final Expression         value;
    public final Hashtable<String, String> tags;
    
    public StaticAttributeDef (
        long                        location,
        String                      id,
        String                      title, 
        String                      comment,
        DataTypeSpec                type,
        Expression                  value,
        Hashtable<String, String>   tags
    ) 
    {
        super (id, title, tags, comment, location);
        
        this.type = type;
        this.value = value;
        this.tags = tags;
    }
    
    public StaticAttributeDef (
        String                      id,
        String                      title, 
        String                      comment,
        DataTypeSpec                type,
        Expression                  value,
        Hashtable<String, String>   tags
    ) 
    {
        this (Location.NONE, id, title, comment, type, value, tags);
    }

    @Override
    public void         print (StringBuilder s) {
        s.append ("STATIC ");
        printHeader (s);
        s.append (' ');
        type.print (s);
        s.append (" = ");
        value.print (s);
        printTags(s);
        printComment (s);
    }        
}