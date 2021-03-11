package com.epam.deltix.qsrv.hf.tickdb.lang.parser;

/**
 *
 */
class EncodingInfo {
    String  encoding;
    int     dimension;

    public EncodingInfo (String encoding, int dimension) {
        this.encoding = encoding;
        this.dimension = dimension;
    }        
}
