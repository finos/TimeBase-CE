package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *  Works with a mix of BoundExternalDecoder and ExternalDecoder
 *  instances.
 */
public class PolyBoundMixedDecoderImpl implements BoundDecoder {
    private BoundExternalDecoder []         decoders;
    
    public PolyBoundMixedDecoderImpl (BoundExternalDecoder [] decoders) {
        this.decoders = decoders; 
    }

    private BoundExternalDecoder    getDecoder (int code) {
        BoundExternalDecoder    decoder = decoders [code];

        if (decoder == null)
            throw new RuntimeException (
                "Decoder for class #" + code +
                " was not created (probably due to unloadable class)"
            );

        return (decoder);
    }

    public Object           decode (MemoryDataInput in) {
        int             code = in.readUnsignedByte ();        
        
        return (((BoundDecoder) getDecoder (code)).decode (in));
    }

    public void             decode (MemoryDataInput in, Object message) {
        int             code = in.readUnsignedByte ();        
        
        getDecoder (code).decode (in, message);
    }

    public void             setStaticFields (Object message) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }    
}
