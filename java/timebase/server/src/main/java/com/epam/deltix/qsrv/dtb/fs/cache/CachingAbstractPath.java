package com.epam.deltix.qsrv.dtb.fs.cache;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.dtb.fs.common.DelegatingAbstractPath;
import com.epam.deltix.qsrv.dtb.fs.common.DelegatingOutputStream;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.collections.ByteArray;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.Assertions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class CachingAbstractPath extends DelegatingAbstractPath<CachingAbstractPath> implements CacheEntryLoader {
    private static final Log LOG = CacheImpl.LOG;

    private final CachingFileSystem cfs;

    CachingAbstractPath(AbstractPath delegate, CachingFileSystem cfs) {
        super(delegate);
        this.cfs = cfs;
    }

    @Override
    protected CachingAbstractPath wrap(AbstractPath path) {
        return cfs.wrap(path);
    }

    @Override
    public AbstractFileSystem getFileSystem() {
        return cfs;
    }


    @Override
    public CachingAbstractPath append(String name) {
        return wrap(delegate.append(name));
    }

    @Override
    public CachingAbstractPath getParentPath() {
        return wrap(delegate.getParentPath());
    }

    @Override
    public InputStream openInput(long offset) throws IOException {
        if (exists()) {
            if (length() <= cfs.getMaxBufferSize()) {
                final CacheEntry cacheEntry = cfs.getCache().checkOut(this);
                if (cacheEntry != null) {
                    ByteArray data = cacheEntry.getBuffer();
                    assert length() == data.getLength();

                    return new ByteArrayInputStream(data.getArray(), data.getOffset() + (int) offset, (int) length()) { //TODO: ObjectPool of ByteArrayInputStreamEx
                        private boolean closed;
                        @Override
                        public void close() throws IOException {
                            if ( ! closed) {
                                closed = true;
                                cfs.getCache().checkIn(cacheEntry); // dec reference counter
                            }
                            super.close();
                        }
                    };
                }
            } else {
                LOG.log(LogLevel.WARN).append("Skipping cache for ").append(getPathString()).append(" filesize:").append(length()).commit();
            }
        }
        //TODO: Make sure we return BufferedInputStream here
        return delegate.openInput(offset);
    }

    @Override
    public OutputStream openOutput(long size) throws IOException {
        //TODO: Make sure we return BufferedOutputStream here
        final OutputStream os = delegate.openOutput(size);

        final CacheEntry cacheEntry = (size > 0) ? cfs.getCache().alloc(size) : null;
        if (cacheEntry != null) { // Collect output and place it in cache (frequent UHF use case)
            return new CachingOutputStream(os, cacheEntry.getBuffer()) {
                boolean closed = false;
                @Override
                public void close() throws IOException {
                    super.close();
                    if ( ! closed) {
                        cfs.getCache().update(getPathString(), cacheEntry);
                        closed = true;
                    }
                }
            };
        } else {
            return new DelegatingOutputStream(os) {
                @Override
                public void close() throws IOException {
                    super.close();
                    cfs.getCache().invalidate(getPathString());
                }
            };
        }
    }

    @Override
    public void moveTo(AbstractPath newPath) throws IOException {
        if (isFile())
            cfs.getCache().rename(this.getPathString(), newPath.getPathString());

        delegate.moveTo(newPath);
    }

    @Override
    public CachingAbstractPath renameTo(String newName) throws IOException {
        if (isFile())
            cfs.getCache().rename(this.getPathString(), delegate.getParentPath () + cfs.getSeparator() + newName);

        return wrap(delegate.renameTo(newName));
    }

    @Override
    public void deleteExisting() throws IOException {
        if (isFile()) {
            cfs.getCache().invalidate(this.getPathString());
        }
        delegate.deleteExisting();
    }

    @Override
    public void deleteIfExists() throws IOException {
        if (isFile()) {
            cfs.getCache().invalidate(this.getPathString());
        }
        delegate.deleteIfExists();
    }

    @Override
    public String toString() {
        return delegate.toString() + " (CW)";
    }

    /// CacheEntryLoader interface

    @Override
    public void load(CacheEntry entry) throws IOException {
        assert exists() : "path exist";

        if (Assertions.ENABLED)
            if (length() > entry.getBuffer().getLength())
                throw new IOException("Entry size " + entry.getBuffer().getLength() + " is not enough to load " + length() + " bytes: " + getPathString());

        try (InputStream is = delegate.openInput(0)){
            ByteArray buffer = entry.getBuffer();
            final int bufferRemaining = IOUtil.readFully(is, buffer.getArray(), buffer.getOffset(), buffer.getLength());
            // TODO: Investigate. In current code the returned value never can be positive.
            if (bufferRemaining > 0)
                LOG.log(LogLevel.WARN).append("Cache entry for [").append(getPathString()).append("] experienced incomplete file read, remaining size:").append(bufferRemaining).commit();
        }
    }

}
