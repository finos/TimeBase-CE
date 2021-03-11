package com.epam.deltix.ramdisk;

import org.junit.Test;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.RAMDisk;

/**
 *  Test without cache, five threads and maximum open files. There is
 *  very little reason that this would fail after the smoke test passes,
 *  other than some trivial RAF Cache synchronization issues.
 */
@Category(RAMDisk.class)
public class Test_RD_3_NoCacheConcurrent extends RAMDiskTestBase {
    @Test (timeout = 5000)
    public void         noCacheSmoke () throws Exception {
        runTest (5, Integer.MAX_VALUE, 0);
    }
}
