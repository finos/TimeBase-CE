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
package com.epam.deltix.qsrv.dtb.fs.alloc;

import com.epam.deltix.gflog.api.AppendableEntry;
import com.epam.deltix.util.collections.ByteArray;
import net.jcip.annotations.GuardedBy;

/**
 * HeapManager implemented using "buddy" memory allocation algorithm. Thread-safe
 */
public class BinaryBuddyHeapManager implements HeapManager {

    @GuardedBy("this")
    private final BinaryBuddyAllocator2 alloc;
    private final byte [] heap;
    private final int maxBlockSize;
    private int usedSize;
    private int usefulSize;
    private int numberOfAllocatedBlocks;

    public BinaryBuddyHeapManager (int minBlockSize, int maxBlockSize) {
        this.maxBlockSize = maxBlockSize;

        alloc = new BinaryBuddyAllocator2(minBlockSize, maxBlockSize);
        heap = new byte[maxBlockSize];
    }

    @Override
    public synchronized boolean allocate(int size, ByteArray block) {
        int blockOffset = alloc.allocate(size);
        if (blockOffset != BinaryBuddyAllocator2.NOT_AVAILABLE) {
            block.setArray(heap, blockOffset, size);
            usedSize += getBlockSize(size); //TODO: Here and in other places: we calculate size twice: here and inside Allocator  :-(
            assert usedSize <= maxBlockSize;
            numberOfAllocatedBlocks++;
            usefulSize += size;
            return true;
        } else {
            block.setLength(0);
            return false;
        }
    }

    /** @return false if given block *definitely* was not allocated by this allocator. If result is true, block might have been allocated or not. */
    protected final boolean       isBelongs(ByteArray block) {
        return block.getArray() == heap;
    }

    @Override
    public void deallocate(ByteArray block) {
        assert block.getArray() == heap;
        final int size = block.getLength();
        final long paddedSize = getBlockSize(size);
        synchronized(this) {
            usedSize -= paddedSize;
            usefulSize -= size;
            assert usedSize >= 0;
            alloc.deallocate(block.getOffset(), size);
            numberOfAllocatedBlocks--;
        }
        block.setArray(null, 0, 0);

    }

    public void defragment (ByteArray block) {
        assert block.getArray() == heap;
        final int size = block.getLength();
        final int originalOffset = block.getOffset();
        synchronized (this) {
            final int newOffset = alloc.defragment(originalOffset, size);
            if (newOffset != BinaryBuddyAllocator.NOT_AVAILABLE) {
                assert newOffset != originalOffset;
                System.arraycopy(heap, originalOffset, heap, newOffset, size);
                block.setArray(heap, newOffset, size);
            }
        }
    }


    @Override
    public long getHeapSize() {
        return maxBlockSize;
    }

    @Override
    public synchronized long getUtilization() {
        return 100L*usedSize / maxBlockSize;
    }

    @Override
    public synchronized void appendTo(AppendableEntry entry) {
        long useful = 100L * usefulSize / maxBlockSize;
        long frag = (usedSize > 0) ? 100L * (usedSize - usefulSize) / usedSize : 0;

        entry.append(numberOfAllocatedBlocks).append(" entries use ").append(getUtilization())
            .append("%, useful content ").append(useful).append("%, fragmentation ").append(frag).append('%');
    }

    public static long getBlockSize(long userSize) {
        long result = 1L;
        while (result < userSize)
            result = result << 1;
        return result;
    }

    @Override
    public synchronized String toString() {
        return alloc.toString();
    }
}