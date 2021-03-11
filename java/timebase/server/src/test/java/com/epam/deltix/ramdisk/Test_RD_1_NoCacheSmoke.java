package com.epam.deltix.ramdisk;

import org.junit.Test;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.RAMDisk;

/**
 *  Test without cache and a single thread.
 */
@Category(RAMDisk.class)
public class Test_RD_1_NoCacheSmoke extends RAMDiskTestBase {
    @Test (timeout = 5000)
    public void         noCacheSmoke () throws Exception {
        runTest (1, 1, 0);
    }
}
