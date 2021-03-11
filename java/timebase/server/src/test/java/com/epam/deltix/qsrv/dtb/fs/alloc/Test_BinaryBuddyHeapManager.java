package com.epam.deltix.qsrv.dtb.fs.alloc;

import com.epam.deltix.util.collections.ByteArray;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class Test_BinaryBuddyHeapManager {
    private BinaryBuddyHeapManager heap = new BinaryBuddyHeapManager (4, 64);

    @Test
    public void testSimple () {
        ByteArray bytes = new ByteArray();
        assertEquals(0, bytes.getLength());
        assertHeapState("0:[] 1:[] 2:[] 3:[] 4:[0]");

        heap.allocate(8, bytes);

        assertEquals(8, bytes.getLength());
        assertHeapState("0:[] 1:[2] 2:[4] 3:[8] 4:[]");

        assertEquals(12, heap.getUtilization()); // = 8 / 64 %

        heap.deallocate(bytes);

        assertEquals(0, bytes.getLength());
        assertHeapState("0:[] 1:[] 2:[] 3:[] 4:[0]");
        assertEquals(0, heap.getUtilization());
    }

    @Test
    public void testStupidBug () {
        ByteArray bytes1 = new ByteArray();
        ByteArray bytes2 = new ByteArray();

        heap.allocate(16, bytes1);
        heap.allocate(16, bytes2);

        assertEquals(16, bytes1.getLength());
        assertEquals(16, bytes2.getLength());
        assertHeapState("0:[] 1:[] 2:[] 3:[8] 4:[]");

        assertEquals(50, heap.getUtilization()); // = 32 / 64 %

        heap.deallocate(bytes1);
        heap.deallocate(bytes2);

        assertEquals(0, bytes1.getLength());
        assertEquals(0, bytes2.getLength());

        assertHeapState("0:[] 1:[] 2:[] 3:[] 4:[0]");
        assertEquals(0, heap.getUtilization());
    }

    private void assertHeapState(String expected) {
        assertEquals(expected, heap.toString());
    }
}
