package com.epam.deltix.qsrv.dtb.fs.cache;


import com.epam.deltix.util.collections.ByteArray;

import java.io.IOException;

interface CacheEntry {
    String getPathString();
    ByteArray getBuffer();
}

/** Interface that helps re-allocate buffer and load file content into the buffer of known size */
interface CacheEntryLoader {

    /** @return path string of entry to be loaded */
    String getPathString();

    /** @return size of file to be loaded */
    long length ();

    /** Loads file into given cache entry */
    void load (CacheEntry entry) throws IOException;
}

/** LRU cache with reference counting */
interface Cache {

    /** @return A cache entry for given path string or <code>null</code> (if file cannot be read or too large to cache).
     * This method increments CacheEntry reference counter, caller must call {@link #checkIn(CacheEntry)} to release returned entry (when not null). */
    CacheEntry checkOut(CacheEntryLoader cacheEntryLoader);

    /**
     * Method to release cache entry previously obtained from {@link #checkOut(CacheEntryLoader)} call.
     * This method decrements CacheEntry reference counter.
     */
     void checkIn(CacheEntry key);

    /** Invalidates cached entry for given path string (if it is cached) */
    void invalidate(String pathString);

    void rename(String fromPathString, String toPathString);

//    /** Preload given file path in cache (if cache has vacant spot). Preloading happens in background. Preloaded pages initially have zero reference counter. */
//    void preload (String pathString);

    /** Clears the cache of all entries (even if some entries are checked out) */
    void clear();

    /** Allocates entry of given size for writing.
     * It will not be visible to others until caller calls {@link #update(String, CacheEntry)}.*/
    CacheEntry alloc(long size);

    /** Registers recently writtien cache entry as available for reading */
    void update(String pathString, CacheEntry cacheEntry);
}


