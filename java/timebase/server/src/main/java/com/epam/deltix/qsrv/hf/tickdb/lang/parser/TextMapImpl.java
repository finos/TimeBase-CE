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
