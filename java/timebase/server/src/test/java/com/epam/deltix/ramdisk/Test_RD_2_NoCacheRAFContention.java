package com.epam.deltix.ramdisk;

import org.junit.Test;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.RAMDisk;

/**
 *  Test without cache, five threads and only two open files will create both
 *  contention for files and some concurrency.
 */
@Category(RAMDisk.class)
public class Test_RD_2_NoCacheRAFContention extends RAMDiskTestBase {
    @Test (timeout = 30000)
    public void         noCacheSmoke () throws Exception {
        runTest (5, 2, 0);
    }
}
