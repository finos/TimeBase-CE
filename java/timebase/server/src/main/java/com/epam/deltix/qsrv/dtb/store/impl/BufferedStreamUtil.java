package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.fs.chunkcache.ChunkCacheInputStream;

import java.io.BufferedInputStream;
import java.io.InputStream;

/**
 * @author Alexei Osipov
 */
class BufferedStreamUtil {
    private BufferedStreamUtil() {
    }

    /**
     * Wraps stream into a {@link BufferedInputStream} if this stream is not already buffered.<p>
     *
     * This method helps to avoid double buffering of data.
     */
    static InputStream wrapWithBuffered(InputStream in) {
        if (in instanceof BufferedInputStream || in instanceof ChunkCacheInputStream) {
            // This is already a buffered stream. Don't wrap it.
            return in;
        } else {
            // Buffer data from this stream
            return new BufferedInputStream(in);
        }
    }
}
