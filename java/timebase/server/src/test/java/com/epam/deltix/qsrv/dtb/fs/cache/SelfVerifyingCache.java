package com.epam.deltix.qsrv.dtb.fs.cache;

import com.epam.deltix.util.collections.ByteArray;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Test only, wraps normal cache and verifies that content is not corrupted. A memory waster.
 */
public class SelfVerifyingCache implements Cache {

    private final Map<String, ByteArray> validContent = new HashMap<>();
    private final Cache delegate;

    public SelfVerifyingCache(Cache delegate) {
        this.delegate = delegate;
    }

    @Override
    public synchronized CacheEntry checkOut(final CacheEntryLoader loader) {

        final ByteArray data = new ByteArray();

        CacheEntry result = delegate.checkOut(new CacheEntryLoader() {
            @Override
            public String getPathString() {
                return loader.getPathString();
            }

            @Override
            public long length() {
                return loader.length();
            }

            @Override
            public void load(CacheEntry entry) throws IOException {
                data.setArray(new byte[(int)loader.length()], 0, (int) loader.length());

                loader.load(new CacheEntry() {
                    @Override
                    public String getPathString() {
                        return loader.getPathString();
                    }

                    @Override
                    public ByteArray getBuffer() {
                        return data;
                    }
                });
                System.arraycopy(data.getArray(), 0, entry.getBuffer().getArray(), entry.getBuffer().getOffset(), data.getLength());
            }
        });

        if (data.getLength() != 0) {
            validContent.put(loader.getPathString(), data);
        } else { // we reused content!
            ByteArray cachedCopy = validContent.get(loader.getPathString());
            assert cachedCopy != null;
            if ( ! cachedCopy.isIdentical(result.getBuffer()))
                throw new RuntimeException( "Content is different! " + loader.getPathString());

        }

        return result;
    }

    @Override
    public synchronized void checkIn(CacheEntry key) {
        assert validContent.containsKey(key.getPathString());
        delegate.checkIn(key);
    }

    @Override
    public synchronized void invalidate(String pathString) {
        validContent.remove(pathString);
        delegate.invalidate(pathString);
    }

    @Override
    public synchronized void rename(String fromPathString, String toPathString) {
        ByteArray entry = validContent.remove(fromPathString);
        if (entry != null) {
            validContent.put(toPathString, entry);
        }
        delegate.rename(fromPathString, toPathString);
    }

    @Override
    public synchronized void clear() {
        validContent.clear();
        delegate.clear();
    }

    @Override
    public CacheEntry alloc(long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(String pathString, CacheEntry cacheEntry) {
        throw new UnsupportedOperationException();
    }
}
