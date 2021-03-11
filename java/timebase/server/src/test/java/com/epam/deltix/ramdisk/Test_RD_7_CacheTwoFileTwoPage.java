package com.epam.deltix.ramdisk;

import org.junit.Test;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.RAMDisk;

/**
 *  Five threads, maximum of two open files and only two pages available
 *  will create contention and some concurrency.
 */
@Category(RAMDisk.class)
public class Test_RD_7_CacheTwoFileTwoPage extends RAMDiskTestBase {
    @Test (timeout = 300000)
    public void         twoTest () throws Exception {
        runTest (5, 2, 2, 60000);
    }
}