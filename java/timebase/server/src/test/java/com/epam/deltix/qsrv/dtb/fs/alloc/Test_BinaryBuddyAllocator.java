package com.epam.deltix.qsrv.dtb.fs.alloc;

import org.junit.Test;

import static org.junit.Assert.*;

public class Test_BinaryBuddyAllocator {

    private AllocatorWrapper alloc = new AllocatorWrapper (128);

    @Test
    public void testTierForSizes() {
        assertEquals(0, BinaryBuddyAllocator.getTierForSize(1));
        assertEquals(1, BinaryBuddyAllocator.getTierForSize(2));
        assertEquals(2, BinaryBuddyAllocator.getTierForSize(3));
        assertEquals(2, BinaryBuddyAllocator.getTierForSize(4));

        assertEquals(6, BinaryBuddyAllocator.getTierForSize(64));
        assertEquals(7, BinaryBuddyAllocator.getTierForSize(128));

        assertEquals(512, BinaryBuddyAllocator.getBlockSize(9));
        assertEquals(256, BinaryBuddyAllocator.getBlockSize(8));
    }

    @Test
    public void allocExceedHeapSize () {
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
        int block = alloc.allocate(128 + 1); // entire heap size + 1
        assertEquals(BinaryBuddyAllocator.NOT_AVAILABLE, block);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }

    @Test
    public void deallocUnknownBlock () {
        int block = alloc.allocate(2);
        assertTrue(block != BinaryBuddyAllocator.NOT_AVAILABLE);
        alloc.deallocate(block, 2);

        // try de-allocating already allocated block
        try {
            alloc.deallocate(block, 2);
            fail("Failed to detect de-allocation of unknown block");
        } catch (AssertionError expected) {
        }

        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }

    @Test
    public void allocFullHeapSize () {
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
        int block = alloc.allocate(128); // entire heap size
        assertEquals(0, block);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[U0]");
        alloc.deallocate(block, 128);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }

    @Test
    public void allocHalfHeapSize () {
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");

        int block1 = alloc.allocate(64); // half of heap size
        assertEquals(0, block1);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[U0,F64] 7:[]");

        int block2 = alloc.allocate(64); // half of heap size
        assertEquals(64, block2);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[U0,U64] 7:[]");

        alloc.deallocate(block1, 64);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[U64,F0] 7:[]");

        alloc.deallocate(block2, 64);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }

    @Test
    public void allocSmallBlock () {
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");

        int blockSize = 3;
        int block = alloc.allocate(blockSize);
        assertEquals(0, block);
        assertState("0:[] 1:[] 2:[U0,F4] 3:[F8] 4:[F16] 5:[F32] 6:[F64] 7:[]");

        alloc.deallocate(block, blockSize);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }

    @Test
    public void allocSeveralSmallBlocks () {
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");

        int block1 = alloc.allocate(4);
        assertEquals(0, block1);
        assertState("0:[] 1:[] 2:[U0,F4] 3:[F8] 4:[F16] 5:[F32] 6:[F64] 7:[]");

        int block2 = alloc.allocate(8);
        assertEquals(8, block2);
        assertState("0:[] 1:[] 2:[U0,F4] 3:[U8] 4:[F16] 5:[F32] 6:[F64] 7:[]");

        int block3 = alloc.allocate(8);
        assertEquals(16, block3);
        assertState("0:[] 1:[] 2:[U0,F4] 3:[U8,U16,F24] 4:[] 5:[F32] 6:[F64] 7:[]");

        alloc.deallocate(block2, 8);
        alloc.deallocate(block1, 4);
        alloc.deallocate(block3, 8);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }

    @Test
    public void allocSmallestBlock () {
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");

        int block1 = alloc.allocate(1);
        assertEquals(0, block1);
        assertState("0:[U0,F1] 1:[F2] 2:[F4] 3:[F8] 4:[F16] 5:[F32] 6:[F64] 7:[]");

        alloc.deallocate(block1, 1);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }

    @Test
    public void allocTwoSmallestBlocks () {
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");

        int block1 = alloc.allocate(1);
        assertEquals(0, block1);
        assertState("0:[U0,F1] 1:[F2] 2:[F4] 3:[F8] 4:[F16] 5:[F32] 6:[F64] 7:[]");

        int block2 = alloc.allocate(1);
        assertEquals(1, block2);
        assertState("0:[U0,U1] 1:[F2] 2:[F4] 3:[F8] 4:[F16] 5:[F32] 6:[F64] 7:[]");

        alloc.deallocate(block1, 1);
        alloc.deallocate(block2, 1);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }

    @Test
    public void allocSizeZeroBlock () {
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");

        int block1 = alloc.allocate(0);
        assertEquals(0, block1);
        assertState("0:[U0,F1] 1:[F2] 2:[F4] 3:[F8] 4:[F16] 5:[F32] 6:[F64] 7:[]");

        int block2 = alloc.allocate(0);
        assertEquals(1, block2);
        assertState("0:[U0,U1] 1:[F2] 2:[F4] 3:[F8] 4:[F16] 5:[F32] 6:[F64] 7:[]");

        alloc.deallocate(block1, 0);
        alloc.deallocate(block2, 0);
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }

    @Test
    public void allocFullCapacityUsingSmallestBlocks () {
        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");

        int [] blocks = new int[128];

        //allocate all
        for (int i=0; i < blocks.length; i++) {
            blocks[i] = alloc.allocate(1);
            assertEquals(i, blocks[i]);
        }

        for (int i=0; i < blocks.length; i++) {
            alloc.deallocate(blocks[i], 1);
        }

        assertState("0:[] 1:[] 2:[] 3:[] 4:[] 5:[] 6:[] 7:[F0]");
    }



    //Alloc (0) = 0
    //Alloc (0) = 1
    //Alloc (0) = 2
    //Alloc (0) = 3
    //Alloc (0) = 4
    //Alloc (0) = 5
    //Alloc (0) = 6
    //Alloc (0) = 7
    //Alloc (0) = 8
    //Alloc (0) = 9
    //Alloc (0) = 10
    //Alloc (0) = 11
    //Alloc (0) = 12
    //Alloc (0) = 13
    //Alloc (0) = 14
    //Alloc (0) = 15
    //Alloc (0) = 16
    //Alloc (0) = 17
    //Alloc (6) = 24
    //Alloc (1) = 18
    //Alloc (17) = 32
    //Alloc (0) = 19
    //Alloc (153) = 256
    @Test
    public void bugSuspect () {
        AllocatorWrapper alloc = new AllocatorWrapper (512);

        for (int i = 0; i <= 17; i++) {
            assertEquals(i, alloc.allocate(1));
        }
        assertEquals (24, alloc.allocate(6));
        assertEquals (18, alloc.allocate(1));
        assertEquals (32, alloc.allocate(17));
        assertEquals (19, alloc.allocate(1));
        assertEquals (256, alloc.allocate(153));
    }


    private void assertState (String expected) {
        assertEquals(expected, alloc.toString());
    }

    private static class AllocatorWrapper {
        private final BinaryBuddyAllocator delegate;
        private final IntStack[] usedlists;

        private AllocatorWrapper(int maxBlockSize) {
            this.delegate = new BinaryBuddyAllocator(maxBlockSize);

            int maxNumberOfTiersToKeep = BinaryBuddyAllocator.getTierForSize(maxBlockSize) + 1;

            usedlists = new IntStack[maxNumberOfTiersToKeep];
            for (int i=0; i < maxNumberOfTiersToKeep; i++)
                usedlists[i] = new IntStack(1 << (maxNumberOfTiersToKeep - i - 1));
        }

        int allocate (int size) {
            int result = delegate.allocate(size);
            if (result != BinaryBuddyAllocator.NOT_AVAILABLE) {
                int tier = BinaryBuddyAllocator.getTierForSize(size);
                usedlists[tier].push(result);
            }
            return result;
        }

        void deallocate(int block, int size) {
            int tier = BinaryBuddyAllocator.getTierForSize(size);
            assertTrue("Unknown block " + block, usedlists[tier].remove(block));

            delegate.deallocate(block, size);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            int tierIndex = 0;
            for (String tier : delegate.toString().split(" ")) {
                if (tierIndex > 0)
                    sb.append(' ');
                sb.append(tierIndex);
                sb.append(':');
                sb.append('[');

                boolean needComma = printIntStack(sb, usedlists[tierIndex].toString(), 'U', false);
                printIntStack(sb, tier, 'F', needComma);

                sb.append(']');
                tierIndex++;
            }
            return sb.toString();

        }

        private boolean printIntStack(StringBuilder sb, String tier, char stateMarker, boolean needComma) {
            String tierBlocks = tier.substring(tier.indexOf('[') + 1, tier.lastIndexOf(']'));
            if ( ! tierBlocks.isEmpty()) {
                for (String freeBlock : tierBlocks.split(",")) {
                    if (needComma)
                        sb.append(',');
                    else
                        needComma = true;
                    sb.append(stateMarker);
                    sb.append(freeBlock);

                }
            }
            return needComma;
        }
    }
}
