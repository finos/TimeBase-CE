package com.epam.deltix.ramdisk;

import org.junit.Ignore;
import org.junit.Test;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.RAMDisk;

/**
 *  Five threads, five open files and enough pages to cache all data.
 */
@Category(RAMDisk.class)
public class Test_RD_5_CacheFull extends RAMDiskTestBase {
    @Test (timeout = 5000)
    @Ignore // TODO: FIX ME
    public void         fullCache () throws Exception {
        int     numThreads = 5;

        runTest (numThreads, Integer.MAX_VALUE, Long.MAX_VALUE);
    }
}
