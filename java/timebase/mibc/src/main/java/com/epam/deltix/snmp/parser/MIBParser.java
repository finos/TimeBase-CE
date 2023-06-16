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
package com.epam.deltix.snmp.parser;

import com.epam.deltix.util.lang.Util;
import java.io.*;
import java_cup.runtime.*;

/**
 *
 */
public class MIBParser {
    public static String    getModuleName (File f) throws IOException {
        FileReader      fr = new FileReader (f);
        
        try {
            Lexer   lexer = new Lexer (new BufferedReader (fr));            
            Symbol  symbol = lexer.next_token ();
            
            if (symbol.sym != Symbols.TypeId)
                return (null);
            
            return ((String) symbol.value);
        } finally {
            Util.close (fr);
        }
    }
    
    public static Module    parse (File f) throws IOException {  
        FileReader      fr = new FileReader (f);
        
        try {
            return (parse (new BufferedReader (fr)));
        } finally {
            Util.close (fr);
        }
    }
    
    public static Module    parse (Reader rd) throws IOException {        
        Scanner     scanner = new Lexer (rd);
        Parser      p = new Parser (scanner); 
        
        try {
            Symbol  s = p.parse ();

            return ((Module) s.value);
        } catch (IOException iox) {
            throw (iox);
        } catch (RuntimeException rx) {
            throw (rx);
        } catch (Exception ux) {
            throw new RuntimeException (ux);
        }
    }
}