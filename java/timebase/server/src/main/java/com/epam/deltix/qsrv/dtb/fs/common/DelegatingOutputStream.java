package com.epam.deltix.qsrv.dtb.fs.common;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.OutputStream;

@ParametersAreNonnullByDefault
public class DelegatingOutputStream extends OutputStream {

    private final OutputStream delegate;

    public DelegatingOutputStream(OutputStream delegate) {
        this.delegate = delegate;
    }


    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        delegate.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int offset, int length) throws IOException {
        delegate.write(b, offset, length);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
        super.close();
    }
}
