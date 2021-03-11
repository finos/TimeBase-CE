package com.epam.deltix.qsrv.dtb.fs.chunkcache;

import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Alexei Osipov
 */
public class SmallFileReadStressTest {
    private static final boolean USE_CACHE = true;

    // Note: this test never stops by itself
    @Test
    public void run() throws Exception {
        LocalFS baseFs = new LocalFS();
        //Azure2FS baseFs = ChunkCacheUtils.getAzure2FS();

        long cacheSizeInBytes = 2 * 1024L * 1024 * 1024; // 2 Gb
        int maxFileSizeInBytes =  1 * 1024 * 1024 * 1024 + 1024; // 1 Gb + 1kb
        //int maxFileSizeInBytes =  Integer.MAX_VALUE / 2;
        //int testFileSizeInBytes =  1 * 1024 * 1024 * 1024 + 12; // 177 Mb + 12 bytes
        int testFileSizeInBytes =  12; // 177 Mb + 12 bytes
        //int testFileSizeInBytes =  Integer.MAX_VALUE / 2;


        ChunkCachingFileSystem fs = null;
        if (USE_CACHE) {
            fs = new ChunkCachingFileSystem(baseFs, cacheSizeInBytes, maxFileSizeInBytes, 0);
            //fs = new CachingFileSystem(localFs, 1, maxFileSizeInBytes);
        } else {
            //fs = baseFs;
        }

        AbstractPath testFolderPath = ChunkCacheTestUtils.createTestFolderPath(fs, baseFs);
        testFolderPath.makeFolderRecursive();
        AbstractPath filePath = ChunkCacheTestUtils.getOrCreateTestFile(testFolderPath, testFileSizeInBytes, false, baseFs);

        while (true) {
            fs.invalidateAll();
            testFileRead(filePath);
        }

        //System.out.println(Arrays.asList(strings));
    }

    private void testFileRead(AbstractPath filePath) throws IOException {
        byte[] readBuffer = new byte[8 * 1024];
        long t0 = System.currentTimeMillis();
        InputStream inputStream = filePath.openInput(0);
        int bytesRead;
        long totalBytesRead = 0;
        while ((bytesRead = inputStream.read(readBuffer)) > 0) {
            totalBytesRead += bytesRead;
        }

        long t1 = System.currentTimeMillis();
        //System.out.println("Total time: " + (t1 - t0) + " ms");
        //System.out.println("Read speed: " + (totalBytesRead * 1000 / 1024 / 1024 / (t1 - t0)));
        inputStream.close();
    }

}