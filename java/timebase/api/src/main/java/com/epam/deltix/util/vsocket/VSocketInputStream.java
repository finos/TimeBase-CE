package com.epam.deltix.util.vsocket;


import java.io.InputStream;
import java.io.IOException;
import java.io.FilterInputStream;

/**
 * Date: Jun 22, 2010
 *
 * @author alex
 */
public class VSocketInputStream extends FilterInputStream {
    private final String socketIdStr;

    private volatile long   bytes = 0;
    private volatile long   completed = 0;
    private volatile long   mark;

    public VSocketInputStream(InputStream delegate, String socketIdStr) {
        super(delegate);
        this.socketIdStr = socketIdStr;
    }

    void                        complete() {
        completed = bytes;
    }

    long                        getBytesRead() {
        return completed;
    }

    long                        getTotalBytes() {
        return bytes;
    }

//    void                        setBytesRead(long value) {
//        completed = bytes = value;
//    }

    @Override
    public int                  read () throws IOException {
        int             count = super.read ();

        if (count >= 0)
            bytes ++;

        return (count);
    }


    @Override
    public int                  read(byte[] b) throws IOException {
        int n = super.read(b);

        if (n >= 0)
            bytes += n;

        return (n);
    }

    @Override
    public int                  read (byte[] b, int off, int len) throws IOException {
        int             n = super.read (b, off, len);
        if (n > 0)
            bytes += n;            

        return (n);
    }

    @Override
    public void                 mark (int readlimit) {
        super.mark (readlimit);
        mark = bytes;
    }

    @Override
    public long                 skip(long n) throws IOException {        
        long skipped = super.skip(n);
        bytes += skipped;
        
        return skipped;
    }


    @Override
    public void                 reset () throws IOException {
        super.reset ();

        if (bytes != mark)
            completed = bytes = mark;
    }

    @Override
    public String toString() {
        return getClass().getName() + socketIdStr;
    }
}
