package com.epam.deltix.qsrv.dtb.fs.alloc;

import com.epam.deltix.gflog.api.AppendableEntry;
import com.epam.deltix.util.collections.ByteArray;

/** Implementation of heap manager that simply uses Java heap - for DEBUGGING purposes only */
public final class JavaHeap implements HeapManager {
    @Override
    public boolean allocate(int size, ByteArray block) {
        byte [] bytes = new byte[size];
        block.setArray(bytes, 0, size);
        return true;
    }

    @Override
    public void deallocate(ByteArray block) {
        block.setArray(null, 0, 0);
    }

    @Override
    public void defragment(ByteArray block) {
        // do nothing
    }

    @Override
    public long getHeapSize() {
        return Runtime.getRuntime().totalMemory();
    }

    @Override
    public long getUtilization() {
        return 100L* Runtime.getRuntime().freeMemory() / Runtime.getRuntime().totalMemory();
    }

    @Override
    public void appendTo(AppendableEntry entry) {
        entry.append("Used ").append(getUtilization()).append('%');
    }
}
