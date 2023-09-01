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
package com.epam.deltix.qsrv.hf.tickdb.lang.parser;

import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.qsrv.hf.tickdb.lang.errors.EmptyProgramException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import java.io.*;
import java_cup.runtime.*;

/**
 *
 */
public class QQLParser {
    public static TextMap       createTextMap () {
        return (new TextMapImpl ());
    }
    
    public static Element        parse (String text, TextMap map) {
        return (parse (new StringReader (text), map));        
    }
    
    public static Element        parse (Reader r, TextMap map) {
        Scanner         scanner = new Lexer (r);
        TextMapBuilder  builder = null;
        
        if (map != null) {                        
            TextMapImpl     mapImpl = (TextMapImpl) map;
        
            mapImpl.clear ();
            
            scanner = builder = new TextMapBuilder (scanner, mapImpl);            
        }
        
        Parser          px = new Parser (scanner);

        try {
            Symbol      ret = px.parse ();

            if (ret.value == null)
                throw new EmptyProgramException ();
            
            return ((Element) ret.value);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException (x);
        } finally {
            if (map != null) 
                builder.finish ();
        }
    }
}