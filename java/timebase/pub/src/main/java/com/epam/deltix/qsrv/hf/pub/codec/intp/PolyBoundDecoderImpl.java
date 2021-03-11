package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *
 */
public class PolyBoundDecoderImpl implements BoundDecoder {
    private BoundDecoder []         decoders;
    
    public PolyBoundDecoderImpl (BoundDecoder [] decoders) {
        this.decoders = decoders; 
    }

    protected final BoundDecoder    getDecoder (int code) {
        BoundDecoder    decoder = decoders [code];

        if (decoder == null)
            throw new RuntimeException (
                "Decoder for class #" + code +
                " was not created (probably due to unloadable class)"
            );

        return (decoder);
    }

    public Object           decode (MemoryDataInput in) {
        int             code = in.readUnsignedByte ();        
        
        return (getDecoder (code).decode (in));
    }

    public void             decode (MemoryDataInput in, Object message) {
        int             code = in.readUnsignedByte ();        
        
        getDecoder (code).decode (in, message);
    }

    public void             setStaticFields (Object message) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }    
}
