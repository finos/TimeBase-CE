package com.epam.deltix.qsrv.dtb.store.codecs;

import com.epam.deltix.qsrv.dtb.fs.alloc.ByteArrayHeap;
import com.epam.deltix.qsrv.dtb.fs.alloc.HeapManager;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 *
 */
public class DeflateDecompressor extends BlockDecompressor {

    private final Inflater      inflater = new Inflater ();

//    public DeflateDecompressor(ByteArrayHeap heap) {
//        super(heap);
//    }

    public void                 inflate (
        byte []                     src,
        int                         srcOffset,
        int                         srcLength,
        byte []                     out,
        int                         outOffset,
        int                         outLength
    )
    {
        inflater.reset ();
        inflater.setInput (src, srcOffset, srcLength);

        int         infLength;

        try {
            infLength = inflater.inflate (out, outOffset, outLength);
        } catch (DataFormatException x) {
            throw new com.epam.deltix.util.io.UncheckedIOException(x);
        }

        if (infLength != outLength)
            throw new com.epam.deltix.util.io.UncheckedIOException( "Inflated " + infLength + " bytes; expected: " + outLength
            );

        if (!inflater.finished ())
            throw new com.epam.deltix.util.io.UncheckedIOException( "Inflated " + infLength + " bytes but did not FINISH");
    }
}
