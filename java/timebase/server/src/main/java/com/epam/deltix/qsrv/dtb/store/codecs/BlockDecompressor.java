package com.epam.deltix.qsrv.dtb.store.codecs;

import com.epam.deltix.util.collections.ByteArray;
import com.epam.deltix.util.io.IOUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public abstract class BlockDecompressor {
    //private Logger              logger = LoggerFactory.getLogger("deltix.dtb");

    protected BlockDecompressor() {
    }

//    public BlockDecompressor(ByteArrayHeap heap) {
//        this.heap = heap;
//    }

    public void                 inflate (
        InputStream                 is,
        int                         inLength,
        byte []                     out,
        int                         outOffset,
        int                         outLength
    )
        throws IOException
    {
        if (inLength == 0)
            return;

        ByteArray buffer = new ByteArray(new byte[inLength]);

        assert buffer.getLength() >= inLength;

        IOUtil.readFully(is, buffer.getArray(), buffer.getOffset(), inLength);
        inflate(buffer.getArray(), buffer.getOffset(), inLength, out, outOffset, outLength);
    }

    public abstract void         inflate (
        byte []                     src,
        int                         srcOffset,
        int                         srcLength,
        byte []                     out,
        int                         outOffset,
        int                         outLength
    );
}
