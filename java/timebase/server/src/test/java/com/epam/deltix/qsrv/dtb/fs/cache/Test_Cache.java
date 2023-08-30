/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.dtb.fs.cache;

import org.junit.Test;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_Cache {
    private final int MAX_NUM_ENTRIES = 5;
    private CacheImpl cache = CacheImpl.create(MAX_NUM_ENTRIES, 256); // with current heuristics this creates BinaryBuddyHeapManager with 128 entries (8 bytes each)

    @Test
    public void simple() {
        assertContent("");
        CacheEntry entry = checkOut("a");
        assertContent("a [1]");
        checkIn(entry);
        assertContent("a [0]");
    }

    @Test
    public void repeatedCheckout() {
        CacheEntry entry = checkOut("a");
        assertSame(entry, checkOut("a"));
        assertSame(entry, checkOut("a"));
        assertContent("a [3]");
        checkIn(entry);
        checkIn(entry);
        checkIn(entry);
        assertContent("a [0]");
    }

    @Test
    public void simpleReuse() {
        CacheEntry entry = checkOut("a");
        checkIn(entry);
        assertContent("a [0]");

        assertSame(entry, checkOut("a"));
        assertContent("a [1]");
        checkIn(entry);

        assertContent("a [0]");
    }

    @Test
    public void cacheExhaustion() {
        CacheEntry [] borrowed = new CacheEntry[MAX_NUM_ENTRIES];
        for (int i = 0; i < MAX_NUM_ENTRIES; i++)
            borrowed[i] = assertEntryNotNull(checkOut("a" + i));

        assertNull(checkOut("XXX"));
        checkOut(borrowed[0].getPathString());
        assertNull(checkOut("XXX"));
        checkIn(borrowed[0]);
        assertNull(checkOut("XXX"));
        checkIn(borrowed[0]);
        assertNotNull(checkOut("XXX"));
    }

    /** Test that cache is capable to defragment itself [by moving unused entries to lower addresses] */
    @Test
    public void testDefragmentationOnCheckIn() {
        final int numSlots = 4;
        cache = new CacheImpl(2 * numSlots, 1, numSlots); // 16 slots 1 byte each

        assertHeapDump("0:[] 1:[] 2:[0]"); // largest tier is free
        CacheEntry block0 = checkOut("BLOCK0");
        assertHeapDump("0:[1] 1:[2] 2:[]");
        CacheEntry block1 = checkOut("BLOCK1");
        assertHeapDump("0:[] 1:[2] 2:[]");
        CacheEntry block2 = checkOut("BLOCK2");
        assertHeapDump("0:[3] 1:[] 2:[]"); // blocks 0,1,2 are used; block 3 is available
        CacheEntry block3 = checkOut("BLOCK3");
        assertHeapDump("0:[] 1:[] 2:[]"); // all blocks are used

        // clean the first entry (prepare space for defragmentation)
        checkInAndInvalidate(block0);
        assertHeapDump("0:[0] 1:[] 2:[]");
        assertEquals(3, block3.getBuffer().getOffset()); // WAS: block address is 3
        cache.checkIn(block3);
        assertHeapDump("0:[3] 1:[] 2:[]"); // block 3 was moved to space previously occupied by block0
        block3 = checkOut("BLOCK3");
        assertEquals(0, block3.getBuffer().getOffset()); // NOW: block address is 0

        // now do clean the second entry
        checkInAndInvalidate(block1);
        assertHeapDump("0:[1,3] 1:[] 2:[]");

        assertEquals(2, block2.getBuffer().getOffset()); // WAS: block address is 2
        cache.checkIn(block2);
        assertHeapDump("0:[] 1:[2] 2:[]"); // block 2 was moved to space previously occupied by block1, free blocks 2 and 3 were merged into free block on second tier
        block2 = checkOut("BLOCK2");
        assertEquals(1, block2.getBuffer().getOffset()); // NOW: block address is 1

        checkInAndInvalidate(block2);
        checkInAndInvalidate(block3);

        assertHeapDump("0:[] 1:[] 2:[0]"); // largest tier is free
    }

    /** Same as above but allocates 2-byte blocks */
    @Test
    public void testDefragmentationOnCheckIn2() {
        final int numSlots = 8;
        cache = new CacheImpl(2 * numSlots, 1, numSlots); // 16 slots 1 byte each

        assertHeapDump("0:[] 1:[] 2:[] 3:[0]"); // largest tier is free
        CacheEntry block0 = checkOut("BLOCK0", 2);
        assertHeapDump("0:[] 1:[2] 2:[4] 3:[]");
        CacheEntry block1 = checkOut("BLOCK1", 2);
        assertHeapDump("0:[] 1:[] 2:[4] 3:[]");
        CacheEntry block2 = checkOut("BLOCK2", 2);
        assertHeapDump("0:[] 1:[6] 2:[] 3:[]"); // blocks 0,1,2 are used; block 3 is available
        CacheEntry block3 = checkOut("BLOCK3", 2);
        assertHeapDump("0:[] 1:[] 2:[] 3:[]"); // all blocks are used

        // clean the first entry (prepare space for defragmentation)
        checkInAndInvalidate(block0);
        assertHeapDump("0:[] 1:[0] 2:[] 3:[]");
        assertEquals(6, block3.getBuffer().getOffset()); // WAS: block address is 6
        cache.checkIn(block3);
        assertHeapDump("0:[] 1:[6] 2:[] 3:[]"); // block 3 was moved to space previously occupied by block0
        block3 = checkOut("BLOCK3", 2);
        assertEquals(0, block3.getBuffer().getOffset()); // NOW: block address is 0

        // now do clean the second entry
        checkInAndInvalidate(block1);
        assertHeapDump("0:[] 1:[2,6] 2:[] 3:[]");

        assertEquals(4, block2.getBuffer().getOffset()); // WAS: block address is 24
        cache.checkIn(block2);
        assertHeapDump("0:[] 1:[] 2:[4] 3:[]"); // block 2 was moved to space previously occupied by block1, free blocks 2 and 3 were merged into free block on second tier
        block2 = checkOut("BLOCK2", 2);
        assertEquals(2, block2.getBuffer().getOffset()); // NOW: block address is 2

        checkInAndInvalidate(block2);
        checkInAndInvalidate(block3);

        assertHeapDump("0:[] 1:[] 2:[] 3:[0]"); // largest tier is free
    }


    @Test
    public void testReuseOfVacantEntries() {
        final int numSlots = 16;
        cache = new CacheImpl (2*numSlots, 1, numSlots); // 16 slots 1 byte each

        CacheEntry [] borrowed = new CacheEntry[numSlots];
        for (int i = 0; i < numSlots; i++)
            borrowed[i] = assertEntryNotNull(checkOut("s" + i));

        // check that we have no more space available
        assertNull(checkOut("XXX"));

        assertHeapDump("0:[] 1:[] 2:[] 3:[] 4:[]"); // no free slots

        // now let's free some slots in the middle
        final int middle = 3;
        final int releasedCount = 5;
        for (int i=0; i < releasedCount; i++) {
            checkIn( borrowed[middle + i]);
            borrowed[middle + i] = null;
        }

        // tracker still shows all slots occupied by released entries (stored in "vacancy" list)
        assertHeapDump("0:[] 1:[] 2:[] 3:[] 4:[]"); // no free slots

        // now all new allocations will re-use cache entries
        for (int i = 0; i < releasedCount; i++)
            assertEntryNotNull(checkOut("extra" + i));

        // check that we have no more space available
        assertNull(checkOut("XXX"));
    }

    @Test
    public void testReuseOfVacantEntries2() {
        final int numSlots = 16;
        cache = new CacheImpl (2*numSlots, 1, numSlots); // 16 slots 1 byte each

        CacheEntry [] borrowed = new CacheEntry[numSlots];
        for (int i = 0; i < numSlots; i++)
            borrowed[i] = assertEntryNotNull(checkOut("ENTRY" + i));

        // check that we have no more space available
        assertNull(checkOut("XXX"));
        assertHeapDump("0:[] 1:[] 2:[] 3:[] 4:[]"); // no free slots

        // check in all
        for (int i = 0; i < numSlots; i++)
            checkIn(borrowed[i]);

        // request large entry that will re-use space previously occupied by entries 1..N
        assertEntryNotNull(checkOut("ENTRY" + numSlots, numSlots));
        assertHeapDump("0:[] 1:[] 2:[] 3:[] 4:[]"); // no free slots

        for (int i = 0; i < numSlots; i++)
            assertNull(checkOut("ENTRY"+i));
    }

    private static CacheEntry assertEntryNotNull (CacheEntry entry) {
        assertNotNull(entry);
        return entry;
    }

    private void assertHeapDump(String expected) {
        assertEquals(expected, cache.heapToString());
    }

    @Test
    public void lruOrder() {
        CacheEntry [] borrowed = new CacheEntry[MAX_NUM_ENTRIES];
        for (int i = 0; i < MAX_NUM_ENTRIES; i++)
            borrowed[i] = checkOut("a" + i);

        checkIn(borrowed[1]);
        checkIn(borrowed[3]);
        checkIn(borrowed[0]);
        checkIn(borrowed[2]);
        checkIn(borrowed[4]);

        // check order of last usage
        assertContent(
            "a1 [0]\n" +
            "a3 [0]\n" +
            "a0 [0]\n" +
            "a2 [0]\n" +
            "a4 [0]");

    }

    @Test(expected = IllegalStateException.class)
    public void unmatchedCheckIn() {
        CacheEntry entry = checkOut("a");
        checkIn(entry);
        checkIn(entry);
    }


    @Test
    public void badRead() {
        assertNull(cache.checkOut(new BadEntryLoader("Bad")));
        assertContent("");
    }

    @Test
    public void rename() {
        checkIn(checkOut("a"));
        checkOut("b");
        assertContent("a [0]\nb [1]");

        cache.rename("a", "A");
        cache.rename("b", "B");
        assertContent("A [0]\nB [1]");
    }

    @Test
    public void invalidate() {
        checkIn(checkOut("a"));
        checkOut("b");
        assertContent("a [0]\nb [1]");
        cache.invalidate("a");
        cache.invalidate("b");
        assertContent("");
    }

    @Test
    public void clear() {
        checkIn(checkOut("a"));
        checkIn(checkOut("b"));
        assertContent("a [0]\nb [0]");
        cache.clear();
        assertContent("");
    }

    @Test(timeout = 100000)
    public void simpleMultiThreadingTest() throws InterruptedException {
        final int numThreads = 15;
        final int numCheckouts = 256;
        final int cacheSize = 12;

        cache = CacheImpl.create(cacheSize, 256);

        Thread [] threads = new Thread[numThreads+1];
        for (int i=0; i < threads.length; i++) {
            final int threadId = i;
            threads[i] = new Thread () {
                @Override
                public void run() {
                    try {
                        Random rnd = new Random(152 << threadId);
                        int cacheIsFullCounter = 0;
                        for (int i=0; i < numCheckouts; i++) {
                            CacheEntry entry = cache.checkOut( new SlowEntryLoader("Page" + (i * threadId) % (2*cacheSize)));
                            Thread.sleep(rnd.nextInt(250));
                            if (entry != null)
                                cache.checkIn(entry);
                            else
                                cacheIsFullCounter++;
                        }
                        System.err.println("Thread " + threadId + " finished: cache was full " + cacheIsFullCounter + "/" + numCheckouts );
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        threads[numThreads] = new Thread() {
            @Override
            public void run() {
                Random rnd = new Random(152);
                try {
                    for (int i=0; i < numCheckouts; i++) {
                        cache.invalidate("Page" + rnd.nextInt(2*cacheSize));
                        Thread.sleep(rnd.nextInt(250));
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };

        for(Thread thread : threads)
            thread.start();

        for(Thread thread : threads)
            thread.join();

    }


    private CacheEntry checkOut(String pathString) {
        return cache.checkOut(new EmptyEntryLoader(pathString));
    }

    private CacheEntry checkOut(String pathString, int size) {
        return cache.checkOut(new EmptyEntryLoader(pathString, size));
    }

    private void checkIn(CacheEntry entry) {
        cache.checkIn(entry);
    }

    private void checkInAndInvalidate(CacheEntry entry) {
        String key = entry.getPathString();
        cache.checkIn(entry);
        cache.invalidate(key);
    }




    private void assertContent(String expectedContent) {
        assertContent(expectedContent, true);
    }

    private void assertContent(String expectedContent, boolean skipEmpty) {
        String actualContent = cache.toString();
        if (skipEmpty) {
            StringBuilder nonEmptyActualContent = new StringBuilder();
            for (String entry : actualContent.split("\n")) {
                if (entry.equals("null [0]"))
                    continue;
                if (nonEmptyActualContent.length() > 0)
                    nonEmptyActualContent.append('\n');
                nonEmptyActualContent.append(entry);
            }
            assertEquals(expectedContent, nonEmptyActualContent.toString());
        }  else {
            assertEquals(expectedContent, actualContent);
        }
    }


    private static abstract class AbstractCacheEntryLoader implements CacheEntryLoader {

        private final String pathString;
        private final int size;

        protected AbstractCacheEntryLoader(String pathString, int size) {
            this.pathString = pathString;
            this.size = size;
        }


        @Override
        public String getPathString() {
            return pathString;
        }

        @Override
        public long length() {
            return size;
        }
    }

    private static class EmptyEntryLoader extends AbstractCacheEntryLoader {



        protected EmptyEntryLoader(String pathString) {
            super(pathString, 1);
        }

        protected EmptyEntryLoader(String pathString, int size) {
            super(pathString, size);
        }

        @Override
        public void load(CacheEntry entry) {
            // do nothing
        }
    }

    private static class SlowEntryLoader extends AbstractCacheEntryLoader {


        protected SlowEntryLoader(String pathString) {
            super(pathString, 1);
        }

        @Override
        public void load(CacheEntry entry) throws IOException {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
        }
    }

    private static class BadEntryLoader extends  AbstractCacheEntryLoader {

        protected BadEntryLoader(String pathString) {
            super(pathString, 1);
        }

        @Override
        public void load(CacheEntry entry) throws IOException {
            throw new IOException("Simulated error");
        }
    }
}