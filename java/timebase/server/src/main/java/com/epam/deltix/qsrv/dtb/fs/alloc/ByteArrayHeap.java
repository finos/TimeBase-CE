package com.epam.deltix.qsrv.dtb.fs.alloc;

import com.epam.deltix.qsrv.hf.blocks.ObjectPool;
import com.epam.deltix.util.collections.ByteArray;

public class ByteArrayHeap {

    private final HeapManager             heap;
    private final ObjectPool<ByteArray>   pool = new ObjectPool<ByteArray>(1000, 10000) {
        @Override
        protected ByteArray newItem() {
            return new ByteArray();
        }
    };

    public ByteArrayHeap(HeapManager heap) {
        this.heap = heap;
    }

    public ByteArray                create (int size) {

        ByteArray block = pool.borrow();
        if (heap.allocate(size, block)) {
            return block;
        } else {
            block.setArray(new byte[size], 0, size);
        }

        return block;
    }

    public void                     free (ByteArray block) {
        heap.deallocate(block);
        pool.release(block);
    }

    public HeapManager              getHeap() {
        return heap;
    }


}
