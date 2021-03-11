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
