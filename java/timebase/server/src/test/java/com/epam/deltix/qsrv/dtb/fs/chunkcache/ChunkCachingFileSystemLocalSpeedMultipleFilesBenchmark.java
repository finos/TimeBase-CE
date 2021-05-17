/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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