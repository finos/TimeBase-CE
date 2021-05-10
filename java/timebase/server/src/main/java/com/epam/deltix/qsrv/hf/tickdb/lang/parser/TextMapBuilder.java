package com.epam.deltix.qsrv.hf.tickdb.lang.parser;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.codec.ClassCodecFactory;
import com.epam.deltix.util.parsers.Location;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.util.lang.Util;
import java.util.logging.Level;
import java_cup.runtime.*;

/**
 *
 */
class TextMapBuilder implements Scanner {
    private static final Log LOG = LogFactory.getLog(TextMapBuilder.class);
    private final Scanner           delegate;
    private final TextMapImpl       map;
    private boolean                 done = false;
    
    public TextMapBuilder (Scanner delegate, TextMapImpl map) {
        this.delegate = delegate;
        this.map = map;
    }
        
    void                            finish () {
        try {
            while (!done)
                next_token ();
        } catch (Exception x) {
            LOG.warn ("Error while finishing a text map: %s").with(x);
        }
    }
    
    public Symbol                   next_token () throws Exception {
        Symbol          symbol = delegate.next_token ();
        TokenType       type = null;
        
        switch (symbol.sym) {
            case Symbols.STRING:
                type = TokenType.STRING_LITERAL;
                break;
                
            case Symbols.UINT:
                type = TokenType.INT_LITERAL;
                break;
                
            case Symbols.FP:
                type = TokenType.FLOAT_LITERAL;
                break;
                
            case Symbols.IDENTIFIER:
                type = TokenType.IDENTIFIER;
                break;
                
            case Symbols.error:
                break;
                
            case Symbols.EOF:
                done = true;
                break;
                
            default:
                if (symbol.sym < Symbols.UNION)
                    type = TokenType.PUNCTUATION;
                else if (symbol.sym >= Symbols.UNION && symbol.sym < Symbols.IDENTIFIER)
                    type = TokenType.KEYWORD;
                break;                    
        }
        
        if (type != null)
            map.add (type, Location.combine (symbol.left, symbol.right));
        
        return (symbol);
    }    
}
