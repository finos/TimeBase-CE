package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.parsers.Location;

/**
 *  Token information used for syntax highlighting.
 */
public class Token {
    /**
     *  Token type.
     */
    public final TokenType          type;
    
    /**
     *  Token location, basically consisting of start and end line and position 
     *  numbers, packed into a 64-bit long integer. Use methods in the 
     * {@link Location} class to decode start and end line and position numbers.
     */
    public final long               location;

    public Token (TokenType type, long location) {
        this.type = type;
        this.location = location;
    }       
    
    @Override
    public String       toString () {
        return (type + ":" + Location.toString (location));
    }
}
