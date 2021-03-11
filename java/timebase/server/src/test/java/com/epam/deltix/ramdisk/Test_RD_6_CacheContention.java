package com.epam.deltix.ramdisk;

import org.junit.Test;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.RAMDisk;

/**
 *  Three threads, maximum of one open file and only one page available
 *  will create maximum contention
 */
@Category(RAMDisk.class)
public class Test_RD_6_CacheContention extends RAMDiskTestBase {
    @Test (timeout = 300000)
    public void         cacheContention () throws Exception {
        runTest (3, 1, 1);
    }
}
