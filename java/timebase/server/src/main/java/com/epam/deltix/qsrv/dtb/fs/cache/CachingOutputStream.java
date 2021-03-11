package com.epam.deltix.qsrv.dtb.fs.cache;

import com.epam.deltix.util.collections.ByteArray;

import java.io.IOException;
import java.io.OutputStream;

class CachingOutputStream extends OutputStream {

    private final OutputStream delegate;
    private final byte [] buffer;
    private final int base;
    private int bytesStored;
    private final int length;

    protected CachingOutputStream(OutputStream delegate, byte [] buffer, int offset, int length) {
        this.delegate = delegate;
        this.buffer = buffer;
        this.base = offset;
        this.length = length;
    }

    public CachingOutputStream(OutputStream os, ByteArray buffer) {
        this(os, buffer.getArray(), buffer.getOffset(), buffer.getLength());
    }

    @Override
    public void write(int b) throws IOException {
        if (bytesStored > length)
            throw new ArrayIndexOutOfBoundsException(bytesStored);

        buffer[base + bytesStored] = (byte) b;
        bytesStored++;

        delegate.write(b);
    }

    @Override
    public void write(byte[] block) throws IOException {
        write(block, 0, block.length);
    }

    @Override
    public void write(byte[] block, int blockOffset, int blockLength) throws IOException {
        if (this.bytesStored + blockLength > this.length)
            throw new ArrayIndexOutOfBoundsException(this.bytesStored);

        System.arraycopy(block, blockOffset, buffer, this.base + this.bytesStored, blockLength);
        this.bytesStored += blockLength;

        delegate.write(block, blockOffset, blockLength);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

}
