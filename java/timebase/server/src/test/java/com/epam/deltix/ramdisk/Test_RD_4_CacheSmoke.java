package com.epam.deltix.ramdisk;

import org.junit.Test;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.RAMDisk;

/**
 *  One thread, file and page - should have minimal synchronization impact.
 *  This should expose obvious bugs in the cache logic and writer thread
 *  concurrency problems.
 */
@Category(RAMDisk.class)
public class Test_RD_4_CacheSmoke extends RAMDiskTestBase {
    @Test (timeout = 300000)
    public void         noCacheSmoke () throws Exception {
        runTest (1, 1, 1);
    }
}