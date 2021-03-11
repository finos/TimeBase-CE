package com.epam.deltix.qsrv.dtb.store.codecs;

import com.epam.deltix.qsrv.dtb.fs.alloc.ByteArrayHeap;
import com.epam.deltix.qsrv.dtb.fs.alloc.HeapManager;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;


/**
 *
 */
public class LZ4BlockDecompressor extends BlockDecompressor {
    private final LZ4Factory                    factory = LZ4Factory.fastestInstance();
    private final LZ4FastDecompressor           decompressor = factory.fastDecompressor();

    public LZ4BlockDecompressor() {
    }

//    public LZ4BlockDecompressor(ByteArrayHeap heap) {
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
        int compressedLength = decompressor.decompress(src, srcOffset, out, outOffset, outLength);
        if (compressedLength != srcLength)
            throw new com.epam.deltix.util.io.UncheckedIOException( "Compressed size " + compressedLength + " bytes; expected: " + srcLength
            );
    }
}
