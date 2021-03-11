package com.epam.deltix.qsrv.dtb.fs.chunkcache;

import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Reads file by accessing individual integers in reverse order.
 *
 * @author Alexei Osipov
 */
public class Test_Write {
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

        AbstractPath cachedFolderPath = ChunkCacheTestUtils.createTestFolderPath(cacheFs, baseFs);

        // Create file without caching FS to avoid cache on write for this test
        String fileName = "test_file_" + "write" + "_" + testFileSizeInBytes + ".blob";
        AbstractPath filePath = cachedFolderPath.append(fileName);
        filePath.deleteIfExists();
        ChunkCacheTestUtils.createTestFile(filePath, testFileSizeInBytes);

        ChunkCacheTestUtils.testFileReadByByteWithValidation(filePath);
    }
}