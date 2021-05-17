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

/**
 *
 */
public final class StaticAttributeDef extends AttributeDef {
    public final TypeIdentifier     typeId;
    public final Expression         value;
    
    public StaticAttributeDef (
        long                        location,
        String                      id,
        String                      title, 
        String                      comment,
        TypeIdentifier              typeId,
        Expression                  value
    ) 
    {
        super (id, title, comment, location);
        
        this.typeId = typeId;
        this.value = value;
    }
    
    public StaticAttributeDef (
        String                      id,
        String                      title, 
        String                      comment,
        TypeIdentifier              typeId,
        Expression                  value
    ) 
    {
        this (Location.NONE, id, title, comment, typeId, value);
    }

    @Override
    public void         print (StringBuilder s) {
        s.append ("STATIC ");
        printHeader (s);
        s.append (' ');
        typeId.print (s);
        s.append (" = ");
        value.print (s);
        printComment (s);
    }        
}
