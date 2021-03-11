package com.epam.deltix.qsrv.dtb.fs.alloc;

import com.epam.deltix.util.collections.ByteArray;

/**
 * HeapManager implemented using "buddy" memory allocation algorithm which never returns false on allocations.
 */
public class BinaryBuddyHeapManager2 extends BinaryBuddyHeapManager {

    public BinaryBuddyHeapManager2(int minBlockSize, int maxBlockSize) {
        super(minBlockSize, maxBlockSize);
    }

    @Override
    public synchronized boolean allocate(int size, ByteArray block) {
        boolean allocated = super.allocate(size, block);
        if (!allocated) {
            System.out.printf("!!! Direct memory allocation for %,d.\n", size);
            block.setArray(new byte[size], 0, size);
        }

        return true;
    }

    @Override
    public synchronized void deallocate(ByteArray block) {
        if (isBelongs(block))
            super.deallocate(block);

        block.setArray(null, 0, 0);
    }
}
