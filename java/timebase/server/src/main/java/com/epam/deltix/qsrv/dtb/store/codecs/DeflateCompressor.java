package com.epam.deltix.qsrv.dtb.store.codecs;

import com.epam.deltix.util.collections.generated.*;
import java.util.zip.*;

/**
 *
 */
public class DeflateCompressor extends BlockCompressor {
    private final Deflater      deflater;

    public DeflateCompressor(int level, ByteArrayList buffer) {
        super(buffer);
        deflater = new Deflater (level);
    }

    @Override
    public byte                 code() {
        return BlockCompressorFactory.getCode(Algorithm.ZLIB);
    }

    public int                  deflate (
        byte []                     src, 
        int                         offset, 
        int                         length, 
        ByteArrayList               appendTo
    )
    {
        deflater.reset ();
        deflater.setInput (src, offset, length);
        deflater.finish ();
        
        int     numBytesOut = 0;
        
        while (!deflater.finished ()) {
            int         size = appendTo.size ();
            
            appendTo.ensureCapacity (size + 4096);
            
            byte []     data = appendTo.getInternalBuffer ();
            
            int         n = deflater.deflate (data, size, data.length - size);
            
            numBytesOut += n;
            
            appendTo.setSize (size + n);
        }
        
        return (numBytesOut);
    }
}
