package com.epam.deltix.ramdisk;

/**
 *
 */
public class GlobalStats {
    long                        numAllocPages;
    long                        numFreePages;
    int                         numOpenVirtualFiles;
    int                         numOpenFiles;
    long                        numPages;
    
    /**
     *  Return the total number of pages allocated.
     */
    public long                 getNumAllocatedPages () {
        return numAllocPages;
    }

    /**
     *  Return the number of pages in the free pool.
     */
    public long                 getNumFreePages () {
        return numFreePages;
    }

    /**
     *  Return the number of mapped pages, i.e. pages actually caching some
     *  data.
     */
    public long                 getNumMappedPages () {
        return (numAllocPages - numFreePages);
    }

    /**
     *  Return the total number of available pages.
     */
    public long                 getNumPages() {
        return numPages;
    }

    /**
     *  Approximate amount of heap consumed by the cache.
     */
    public long                 getUsedMemory () {
        return (numAllocPages * DataCache.PAGE_SIZE_WITH_OVERHEAD);
    }

    /**
     *  Approximate amount of data cached.
     */
    public long                 getCachedMemory () {
        return (getNumMappedPages () << DataCache.PAGE_SIZE_LOG2);
    }

    /**
     *  Number of currently open files (at OS level).
     */
    public int                  getNumOpenFiles () {
        return numOpenFiles;
    }

    /**
     *  Number of currently open virtual files.
     */
    public int                  getNumOpenVirtualFiles () {
        return numOpenVirtualFiles;
    }
}
