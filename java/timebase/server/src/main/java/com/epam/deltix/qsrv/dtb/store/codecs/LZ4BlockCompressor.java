package com.epam.deltix.qsrv.dtb.store.codecs;

import com.epam.deltix.util.collections.generated.ByteArrayList;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

/**
 *
 */
public class LZ4BlockCompressor extends BlockCompressor {
    private final LZ4Compressor               compressor;

    public LZ4BlockCompressor(int compressionLevel, ByteArrayList buffer) {
        super(buffer);

        if (compressionLevel == 1)
            compressor = LZ4Factory.fastestInstance().fastCompressor();
        else
            compressor = LZ4Factory.fastestInstance().highCompressor(compressionLevel - 1);
    }

    @Override
    public byte                 code() {
        return BlockCompressorFactory.getCode(Algorithm.LZ4);
    }

    public int                  deflate (
        byte []                     src,
        int                         offset,
        int                         length,
        ByteArrayList               appendTo
    )
    {
        int maxCompressedLength = compressor.maxCompressedLength(length);

        int size = appendTo.size();
        appendTo.ensureCapacity(size + maxCompressedLength);
        byte[] data = appendTo.getInternalBuffer();
        int compressedLength = compressor.compress(src, offset, length, data, size, maxCompressedLength);
        appendTo.setSize(size + compressedLength);

        return compressedLength;
    }
}
