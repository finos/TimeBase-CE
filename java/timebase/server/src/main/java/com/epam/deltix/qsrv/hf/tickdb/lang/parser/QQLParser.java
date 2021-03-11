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
