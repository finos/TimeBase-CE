package com.epam.deltix.ramdisk;

import org.junit.Test;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.RAMDisk;

/**
 *  Five threads, max open files and pages to cache one third of all data.
 */
@Category(RAMDisk.class)
public class Test_RD_8_CacheRealistic extends RAMDiskTestBase {
    @Test (timeout = 5000)
    public void         realistic () throws Exception {
        int     numThreads = 5;

        runTest (numThreads, Integer.MAX_VALUE, MAX_NUM_PAGES * numThreads / 3);
    }
}
