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
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

/**
 *
 */
public final class CreateStreamStatement extends Statement {
    public final Identifier             id;
    public final String                 title;
    public final String                 comment;
    public final StreamScope            scope;
    public final OptionElement []       options;
    public final ClassDef []            members;

    public CreateStreamStatement (
        long                        location,
        Identifier                  id, 
        String                      title,                                  
        String                      comment, 
        StreamScope                 scope, 
        OptionElement []            options,
        ClassDef []                 members 
    )
    {
        super (location);
        
        this.scope = scope;
        this.id = id;
        this.title = title;
        this.comment = comment;
        this.options = options;
        this.members = members;
    }
    
    public CreateStreamStatement (
        Identifier                  id, 
        String                      title,                                  
        String                      comment, 
        StreamScope                 scope, 
        OptionElement []            options,
        ClassDef []                 members
    )
    {
        this (
            Location.NONE, 
            id, title, comment, 
            scope, options, members
        );
    }
    
    @Override
    public void                     print (StringBuilder s) {
        s.append ("CREATE ");
        s.append (scope);
        s.append (" STREAM ");
        id.print (s);
        
        if (title != null) {
            s.append (" ");
            GrammarUtil.escapeStringLiteral (title, s);
        }
        
        s.append (" (\n");
        
        boolean     first = true;

        for (ClassDef cd : members) {
            if (first)
                first = false;
            else
                s.append (";\n");

            cd.print (s);
        }

        s.append (")");
        
        OptionElement.print (options, s);
        
        if (comment != null) {
            s.append ("\nCOMMENT ");
            GrammarUtil.escapeStringLiteral (comment, s);
        }
    }        
}