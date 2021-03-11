package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.util.codec.HexCharBinDecoder;

/**
 *  DATE '...' literal.
 */
public final class BinConstant extends Constant {
    public final String             text;
    public final byte []            bytes;
    
    public BinConstant (long location, String text) {
        super (location);
        
        this.text = text;
        this.bytes = HexCharBinDecoder.decode (text);
    }

    protected void      print (int outerPriority, StringBuilder s) {
        GrammarUtil.escapeStringLiteral (text, s);
        s.append ('X');
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            text.equals (((BinConstant) obj).text)
        );
    }

    @Override
    public int                      hashCode () {
        return (super.hashCode () * 41 + text.hashCode ());
    }
}
