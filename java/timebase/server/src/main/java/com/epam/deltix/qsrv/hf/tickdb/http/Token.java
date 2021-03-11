package com.epam.deltix.qsrv.hf.tickdb.http;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TokenType;
import com.epam.deltix.util.parsers.Location;

import javax.xml.bind.annotation.XmlElement;

public class Token {
    /**
     *  Token type.
     */
    @XmlElement()
    public TokenType type;

    /**
     *  Token location, basically consisting of start and end line and position
     *  numbers, packed into a 64-bit long integer. Use methods in the
     * {@link Location} class to decode start and end line and position numbers.
     */
    @XmlElement()
    public long               location;

    public Token() {
    }

    public Token (TokenType type, long location) {
        this.type = type;
        this.location = location;
    }

    @Override
    public String       toString () {
        return (type + ":" + Location.toString (location));
    }
}
