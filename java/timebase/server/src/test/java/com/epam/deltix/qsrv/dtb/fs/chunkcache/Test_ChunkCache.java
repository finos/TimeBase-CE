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

import com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool.Chunk;
import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Alexei Osipov
 */
@Category(TickDBFast.class)
public class Test_ChunkCache {
    @Test
    public void rename() throws Exception {

        LocalFS localFS = new LocalFS();

        ChunkCache chunkCache = new ChunkCache(16 * 1024 * 1024, 1024 * 1024, 1);

        AbstractPath testFolder = ChunkCacheTestUtils.createTestFolderPath(localFS, localFS);
        AbstractPath path1 = testFolder.append("file1");
        AbstractPath path2 = testFolder.append("file2");

        // Create entry
        ChunkCacheFsEntry entry1 = chunkCache.getOrCreateEntry(path1);
        Chunk chunk1 = chunkCache.allocateNewChunk();
        entry1.putChunk(0, chunk1, 42, chunkCache.getChunkPool());

        // Perform rename
        chunkCache.rename(path1, path2);

        // Assert that new path has cached data
        ChunkCacheFsEntry entry2 = chunkCache.getOrCreateEntry(path2);
        Chunk chunk2 = entry2.getChunk(0);

        Assert.assertTrue(chunk1 == chunk2);
    }

}