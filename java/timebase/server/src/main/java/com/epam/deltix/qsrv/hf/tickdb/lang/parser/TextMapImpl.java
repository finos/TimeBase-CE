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
package com.epam.deltix.qsrv.hf.tickdb.lang.parser;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import java.util.ArrayList;

/**
 *
 */
class TextMapImpl implements TextMap {
    private final ArrayList <Token>     tokens = new ArrayList <Token> ();
    
    void        clear () {
        tokens.clear ();
    }
    
    void        add (TokenType type, long location) {
        tokens.add (new Token (type, location));
    }
    
    @Override
    public String       toString () {
        StringBuilder   sb = new StringBuilder ();
        
        for (Token t : tokens) {
            if (sb.length () > 0)
                sb.append (", ");
            
            sb.append (t);
        }
        
        return (sb.toString ());
    }
    
    public Token []     getTokens () {
        return (tokens.toArray (new Token [tokens.size ()]));
    }
}
