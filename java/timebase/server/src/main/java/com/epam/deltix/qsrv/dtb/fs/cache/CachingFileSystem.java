package com.epam.deltix.qsrv.dtb.fs.cache;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.lang.Wrapper;

public final class CachingFileSystem implements AbstractFileSystem, Wrapper<AbstractFileSystem> {

    private final AbstractFileSystem delegate;
    private final Cache cache;
    private long maxBufferSize;

    public CachingFileSystem(AbstractFileSystem delegate, int size, int maxEntrySize) {
        this.delegate = delegate;
        this.maxBufferSize = maxEntrySize;
        this.cache = CacheImpl.create(size, maxEntrySize);
    }

    Cache getCache() {
        return cache;
    }

    public void clear () {
        cache.clear();
    }

    @Override
    public AbstractFileSystem getNestedInstance() {
        return delegate;
    }

    /// AbstractFileSystem interface

    @Override
    public boolean isAbsolutePath(String path) {
        return delegate.isAbsolutePath(path);
    }

    @Override
    public AbstractPath createPath(String path) {
        return wrap(delegate.createPath(path));
    }

    @Override
    public AbstractPath createPath(AbstractPath parent, String child) {
        return wrap(delegate.createPath(parent, child));
    }

    CachingAbstractPath wrap(AbstractPath path) {
        return new CachingAbstractPath(path, this);
    }


    long getMaxBufferSize() {
        return maxBufferSize;
    }

    @Override
    public long getReopenOnSeekThreshold() {
        // Caching file system always loads full file in buffer.
        // Skip operation is fast in it so there is no reason to re-open file.
        return 0;
    }

    @Override
    public int getPrefetchSize() {
        return delegate.getPrefetchSize();
    }

    @Override
    public String getSeparator() {
        return delegate.getSeparator();
    }
}
