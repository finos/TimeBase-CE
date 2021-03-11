package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 * Timebase RAM Disk options.
 */
public class DataCacheOptions {

    public static final long  DEFAULT_CACHE_SIZE = 100*1024*1024;

    public int          maxNumOpenFiles = Integer.MAX_VALUE;
    public long         cacheSize = DEFAULT_CACHE_SIZE;
    public double       preallocateRatio = 0;
    public long         shutdownTimeout = Long.MAX_VALUE;
    public FSOptions    fs = new FSOptions(); // File-System related options

    public DataCacheOptions() {
    }

    public DataCacheOptions(int maxNumOpenFiles, long cacheSize) {
        this(maxNumOpenFiles, cacheSize, 0);
    }

    public DataCacheOptions(int maxNumOpenedFiles, long cacheSize, double preallocateRatio) {
        this.maxNumOpenFiles = maxNumOpenedFiles;
        this.cacheSize = cacheSize;
        this.preallocateRatio = preallocateRatio;
    }

    public long         getInitialCacheSize() {
        return (long) (cacheSize * preallocateRatio);
    }
}
