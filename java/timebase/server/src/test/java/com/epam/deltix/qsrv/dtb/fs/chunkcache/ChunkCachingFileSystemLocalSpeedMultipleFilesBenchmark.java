package com.epam.deltix.qsrv.dtb.fs.chunkcache;

import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Alexei Osipov
 */
public class ChunkCachingFileSystemLocalSpeedMultipleFilesBenchmark {
    private static final boolean USE_CACHE = true;

    @Test
    public void run() throws Exception {
        LocalFS baseFs = new LocalFS();

        long cacheSizeInBytes = 2 * 1024L * 1024 * 1024; // 2 Gb
        int maxFileSizeInBytes =  1 * 1024 * 1024 * 1024 + 1024; // 1 Gb + 1kb
        int testFileSizeInBytes =  1 * 1024 * 1024 * 1024 + 12; // 1 Gb Mb + 12 bytes

        AbstractFileSystem fs;
        if (USE_CACHE) {
            fs = new ChunkCachingFileSystem(baseFs, cacheSizeInBytes, maxFileSizeInBytes, 0);
        } else {
            fs = baseFs;
        }

        AbstractPath testFolderPath = ChunkCacheTestUtils.createTestFolderPath(fs, baseFs).append("multiple");

        AbstractPath filePath1 = ChunkCacheTestUtils.getOrCreateTestFile(testFolderPath, testFileSizeInBytes, "1", false, baseFs);
        AbstractPath filePath2 = ChunkCacheTestUtils.getOrCreateTestFile(testFolderPath, testFileSizeInBytes, "2", false, baseFs);
        AbstractPath filePath3 = ChunkCacheTestUtils.getOrCreateTestFile(testFolderPath, testFileSizeInBytes, "3", false, baseFs);

        Thread.sleep(10000);

        int passNumber = 0;
        while (passNumber < 10) {
            passNumber++;
            System.out.println("==================================");
            System.out.println("Pass: " + passNumber);
            System.out.println("==================================");
            System.out.println("First read");
            testFileRead(filePath1, testFileSizeInBytes);
            System.out.println("Second read");
            testFileRead(filePath2, testFileSizeInBytes);
            System.out.println("Third read");
            testFileRead(filePath3, testFileSizeInBytes);
        }

        //System.out.println(Arrays.asList(strings));
    }

    private void testFileRead(AbstractPath filePath, long testFileSizeInBytes) throws IOException {
        long t0 = System.currentTimeMillis();
        ChunkCacheTestUtils.testFileReadByByteWithValidation(filePath, testFileSizeInBytes);

        long t1 = System.currentTimeMillis();
        System.out.println("Total time: " + (t1 - t0) + " ms");
        System.out.println("Read speed: " + (testFileSizeInBytes * 1000 / 1024 / 1024 / (t1 - t0))  + " Mb/s");
    }

}