package com.epam.deltix.qsrv.hf.tickdb.impl.stream;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link BufferedOutputStream} that can be re-used with {@link #reuse(OutputStream)} method.
 *
 * @author Alexei Osipov
 */
public class ReusableBufferedOutputStream extends BufferedOutputStream {
    public ReusableBufferedOutputStream(@Nonnull OutputStream out) {
        super(out);
    }

    public ReusableBufferedOutputStream(@Nonnull OutputStream out, int size) {
        super(out, size);
    }

    /**
     * Wraps new output stream instead of previously wrapped.
     * @return new wrapped stream
     */
    public ReusableBufferedOutputStream reuse(@Nonnull OutputStream out) {
        this.out = out;
        this.count = 0;
        return this;
    }

    /**
     * This implementation is equal to {@link FilterOutputStream#close()} implementation, but ignoring
     * added in java 9+ 'closed' flag.
     */
    @Override
    public void close() throws IOException {
        Throwable flushException = null;
        try {
            flush();
        } catch (Throwable e) {
            flushException = e;
            throw e;
        } finally {
            if (flushException == null) {
                out.close();
            } else {
                try {
                    out.close();
                } catch (Throwable closeException) {
                    // evaluate possible precedence of flushException over closeException
                    if ((flushException instanceof ThreadDeath) &&
                            !(closeException instanceof ThreadDeath)) {
                        flushException.addSuppressed(closeException);
                        throw (ThreadDeath) flushException;
                    }

                    if (flushException != closeException) {
                        closeException.addSuppressed(flushException);
                    }

                    throw closeException;
                }
            }
        }
        this.out = null; // Clear ref to avoid memory leak
    }
}
