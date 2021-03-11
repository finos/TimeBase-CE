package com.epam.deltix.qsrv.dtb.fs.chunkcache;

import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.DataInputStream;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Reads file by accessing individual integers in reverse order.
 *
 * @author Alexei Osipov
 */
public class Test_ReverseReadCorrectness {
    @Test
    @Category(TickDBFast.class)
    public void testLocal() throws Exception {
        LocalFS localFs = new LocalFS();

        testForFs(localFs);
    }

    private void testForFs(AbstractFileSystem baseFs) throws IOException {
        long cacheSizeInBytes = 1024 * 1024 * 1024; // 1 GB
        int maxFileSizeInBytes = 200 * 1024 * 1024; // 200 Mb
        int testFileSizeInBytes = 1024 * 1024 + 12; // 1 Mb + 12 bytes

        ChunkCachingFileSystem cacheFs = new ChunkCachingFileSystem(baseFs, cacheSizeInBytes, maxFileSizeInBytes, 0);

        AbstractPath nonCachedFolderPath = ChunkCacheTestUtils.createTestFolderPath(baseFs, baseFs);
        AbstractPath cachedFolderPath = ChunkCacheTestUtils.createTestFolderPath(cacheFs, baseFs);

        // Create file without caching FS to avoid cache on write for this test
        AbstractPath filePathNonCached = ChunkCacheTestUtils.getOrCreateTestFile(nonCachedFolderPath, testFileSizeInBytes, false, baseFs);
        assertTrue(filePathNonCached.exists());
        AbstractPath filePath = ChunkCacheTestUtils.getOrCreateTestFile(cachedFolderPath, testFileSizeInBytes, false, baseFs);

        long offset = testFileSizeInBytes;
        while (offset > 0) {
            offset -= Integer.BYTES;
            DataInputStream din = new DataInputStream(filePath.openInput(offset));
            int intValue = din.readInt();
            int expectedValue = (int) (offset / Integer.BYTES) + 1;
            Assert.assertEquals(expectedValue, intValue);
            din.close();
        }
    }

}