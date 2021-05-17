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
import com.epam.deltix.qsrv.dtb.fs.chunkcache.chunkpool.ChunkPool;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Alexei Osipov
 */
@Category(TickDBFast.class)
public class Test_ChunkCacheFsEntry {
    private final ChunkPool pool = new ChunkPool(1);

    // http://rm.orientsoft.by/issues/7281
    @Test
    public void getMissingChunkBeforeTheLast() throws Exception {
        ChunkCacheFsEntry entry = new ChunkCacheFsEntry(ChunkCacheFsEntry.CHUNK_SIZE * 128);

        putChunkAt(entry, 10);
        assertNull(entry.getChunk(5));
    }

    @Test
    public void getNumberOfMissingChunks() throws Exception {
        ChunkCacheFsEntry entry = new ChunkCacheFsEntry(ChunkCacheFsEntry.CHUNK_SIZE * 128);

        assertEquals(10, entry.getNumberOfMissingChunks(0, 10));
        assertEquals(10, entry.getNumberOfMissingChunks(5, 10));
        assertEquals(10, entry.getNumberOfMissingChunks(10, 10));

        putChunkAt(entry, 0);
        assertEquals(0, entry.getNumberOfMissingChunks(0, 10));
        assertEquals(10, entry.getNumberOfMissingChunks(1, 10));

        putChunkAt(entry, 7);
        assertEquals(0, entry.getNumberOfMissingChunks(7, 10));
        assertEquals(1, entry.getNumberOfMissingChunks(6, 10));
        assertEquals(6, entry.getNumberOfMissingChunks(1, 10));
        assertEquals(0, entry.getNumberOfMissingChunks(0, 10));
        assertEquals(10, entry.getNumberOfMissingChunks(8, 10));

        putChunkAt(entry, 10);
        assertEquals(2, entry.getNumberOfMissingChunks(8, 10));
        assertEquals(10, entry.getNumberOfMissingChunks(11, 10));
    }

    private void putChunkAt(ChunkCacheFsEntry entry, int chunkIndex) {
        Chunk chunk = new Chunk();
        chunk.allocate();
        entry.putChunk(chunkIndex, chunk, ChunkCacheFsEntry.CHUNK_SIZE, pool);
    }

}