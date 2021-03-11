package com.epam.deltix.qsrv.dtb.fs.chunkcache;

import com.epam.deltix.qsrv.dtb.fs.azure2.Azure2FS;
import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.io.IOUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads first int and last int from file and check value correctness.
 *
 * @author Alexei Osipov
 */
public class Test_ChunkCachingFsCorrectness {

    @Test
    public void testLocalWrite() throws Exception {
        LocalFS localFs = new LocalFS();

        testForFs(localFs, true);
    }

    @Test
    public void testLocal() throws Exception {
        LocalFS localFs = new LocalFS();

        testForFs(localFs, false);
    }

    @Test
    @Ignore // required Azure setup and can be slow
    public void testAzure() throws Exception {
        Azure2FS baseFs = ChunkCacheTestUtils.getAzure2FS();

        testForFs(baseFs, false);
    }

    private void testForFs(AbstractFileSystem baseFs, boolean requireNew) throws IOException {
        long cacheSizeInBytes = 1024 * 1024 * 1024; // 1 GB
        int maxFileSizeInBytes = 200 * 1024 * 1024; // 200 Mb
        int testFileSizeInBytes = 177 * 1024 * 1024 + 12; // 177 Mb + 12 bytes

        ChunkCachingFileSystem cacheFs = new ChunkCachingFileSystem(baseFs, cacheSizeInBytes, maxFileSizeInBytes, 0);

        AbstractPath testFolderPath = ChunkCacheTestUtils.createTestFolderPath(cacheFs, baseFs);
        AbstractPath filePath = ChunkCacheTestUtils.getOrCreateTestFile(testFolderPath, testFileSizeInBytes, requireNew, baseFs);

        long t0 = System.currentTimeMillis();
        InputStream inputStream = filePath.openInput(0);
        DataInputStream din = new DataInputStream(inputStream);
        int firstInt = din.readInt();
        Assert.assertEquals(1, firstInt);
        IOUtil.skipFully(din, testFileSizeInBytes - Integer.BYTES * 2);
        int lastInt = din.readInt();
        int expectedValue = testFileSizeInBytes / Integer.BYTES;
        Assert.assertEquals(expectedValue, lastInt);
        Assert.assertEquals(-1, din.read()); // Assert no more data
        long t1 = System.currentTimeMillis();
        System.out.println("Total time: " + (t1 - t0) / 1000);
    }

}