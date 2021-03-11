package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

/**
 *  Provides information about the location of various tokens in a QQL program.
 */
public interface TextMap {
    /**
     *  Returns an array of tokens, ordered by location.
     */
    public Token []     getTokens ();
}
