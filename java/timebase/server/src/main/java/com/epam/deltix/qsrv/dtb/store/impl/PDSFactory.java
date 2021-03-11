package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.store.pub.PersistentDataStore;
import com.epam.deltix.util.concurrent.QuickExecutor;

/**
 *
 */
public class PDSFactory {

    public static final int             DEFAULT_HEAP_SIZE = 1 << 28;
    public static final int             MAX_HEAP_SIZE = 1 << 29;

//    private static HeapManager          heap;
//    private static ByteArrayHeap        allocator;

//    public static synchronized void             allocate (int cacheSize) {
//
//        int log2 = (int) (Math.log10(cacheSize) / Math.log10(2));
//        int size = (int) Math.min(Math.pow(2, log2), MAX_HEAP_SIZE);
//
//        if (heap == null) {
//            heap = new BinaryBuddyHeapManager2(512, size);
//            PDSImpl.LOGGER.level(Level.INFO).append("Allocating Heap: " ).append(size >> 20).append(" MB").commit();
//        }
//
//        if (allocator == null)
//            allocator = new ByteArrayHeap(heap);
//    }

//    public static ByteArrayHeap getAllocator() {
//        return allocator;
//    }

    /*
        Create a persistent data store
     */
    public static synchronized PersistentDataStore          create (QuickExecutor exe) {
        return (new PDSImpl (exe));
    }

    public static synchronized PersistentDataStore          create () {
        return (new PDSImpl ());
    }
}
