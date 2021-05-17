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