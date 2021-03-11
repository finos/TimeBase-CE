package com.epam.deltix.qsrv.dtb.fs.alloc;


import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.gflog.LogLevel;
import com.epam.deltix.util.lang.Assertions;

import java.util.BitSet;

/**
 *
 * https://en.wikipedia.org/wiki/Buddy_memory_allocation
 * http://homepage.cs.uiowa.edu/~jones/opsys/notes/27.shtml
 */
final class BinaryBuddyAllocator {
    private static final Log LOG = LogFactory.getLog(BinaryBuddyAllocator.class);
    private static final boolean DEBUG = LOG.isEnabled(LogLevel.DEBUG);

    static boolean  ASSERTIONS_ENABLED = Assertions.ENABLED;

    public static final int NOT_AVAILABLE = -1;

    /* pointers to the free space lists (smallest blocks occupy start of array) */
    private final Tracker blockTracker;

    /**
     * @param maxBlockSize maximum allocation unit (equals to entire heap size this implementation manage).
     */
    BinaryBuddyAllocator(int maxBlockSize)
    {
        if (Integer.bitCount(maxBlockSize) != 1)
            throw new IllegalArgumentException("maxBlockSize must be a power of 2");

        final int maxNumberOfTiersToKeep = getTierForSize(maxBlockSize) + 1;
        blockTracker = new BitSetTracker(maxNumberOfTiersToKeep);
    }

    /** @return offset of allocated block of requested size (note allocated block may be larger than requested, but caller is not allowed to use extra space).
     * Method returns {@link #NOT_AVAILABLE} if this allocator has no free space to accommodate this request.*/
    int allocate (int size) {
        int result = allocateSmallerAddress(getTierForSize(size), NOT_AVAILABLE);
        if (DEBUG)
            LOG.log(LogLevel.DEBUG).append("(CCC) Alloc (").append(size).append(") = ").append(result).commit();
        return result;
    }

    /**
     * Find free block at given tier. If none found try to allocate half of block of larger size
     * @param lessThanBlock if this parameter specified method must search only among blocks of smaller address (result < lessThanBlock)
     * @return block address available for consumption
     */
    private int allocateSmallerAddress (final int tier, final int lessThanBlock) {
        if (tier >= blockTracker.getNumberOfTiers())
            return NOT_AVAILABLE; // Block is too large

        int block = blockTracker.removeAny(tier, lessThanBlock); // Check if we already have the right size block at hand
        if (block == NOT_AVAILABLE && lessThanBlock == NOT_AVAILABLE) {
            // we need to split a bigger block
            block = allocateSmallerAddress(tier + 1, NOT_AVAILABLE); //TODO: Consider using lessThanBlock parameter here... (v2)
            if (block != NOT_AVAILABLE) {
                // split and put extra on a free list
                int buddy = getBuddyOf(block, tier);
                blockTracker.add(tier, buddy);
            }
        }
        return block;
    }

    /**
     * @param block block previously returned by {@link #allocate(int)}
     * @param size block size (must be exactly the same as was requested)
     */
    void deallocate(int block, int size) {
        if (DEBUG)
            LOG.log(LogLevel.DEBUG).append("(CCC) Dealloc (").append(size).append(") = ").append(block).commit();

        final int tier = getTierForSize(size);

        /* see if this block's buddy is free */
        int buddy = getBuddyOf(block, tier);

        if ( blockTracker.remove(tier, buddy)) { // if buddy found, remove it from its free list
            // deallocate the block and its buddy as one block
            if (block > buddy)
                deallocate(buddy, getBlockSize(tier + 1));
            else
                deallocate(block, getBlockSize(tier + 1));
        } else {
            blockTracker.add(tier, block); // buddy not free, put block on its free list
        }
    }

    /**
     * Relocates given block to the "left" - to free block with the smallest address.
     * @return new offset for the block. We expect caller to move data from block identified by input parameter to
     *         block identified by result. Method returns {@link #NOT_AVAILABLE} if this allocator has no free space to accommodate this request.
     */
    int defragment(int block, int size) {
        int result = allocateSmallerAddress(getTierForSize(size), block);
        if (result != NOT_AVAILABLE) {
            if (DEBUG)
                LOG.log(LogLevel.DEBUG).append("Moved block of size ").append(size).append(" from #").append(block).append(" to #").append(result).commit();
            deallocate(block, size);
        }
        return result;
    }

    /** @return compute i as the least integer such that i >= log2(size) */
    static int getTierForSize(int size) {
        int tier = 0;
        while (getBlockSize(tier) < size)
            tier++;
        return tier;
    }

    /** @return blocks in freelists[i] are of size 2**i. */
    static int getBlockSize(int tier) {
        return 1 << tier;
    }

    /** @return the address of the buddy of a block from freelists[i]. */
    private static int getBuddyOf(int block, int tier) {
        return block ^ (1 << tier);
    }

    @Override
    public String toString() {
        return blockTracker.toString();
    }


    /** Free Block Tracker organize blocks by tier: highest tier hold largest block, tier 0 tracks smallest blocks */
    interface Tracker {
        int getNumberOfTiers();

        /** Marks block as free */
        void add(int tier, int block);

        /**
         * Method tries to remove given block from given tier.
         * @return true if block was free [and successfully removed]
         */
        boolean remove(int tier, int block);

        /**
         * Remove first available block from given tier.
         * @param tier remove first available block from free list of given tier
         * @param lessThanBlock (if specified) than result should be less than
         * @return Returns block or NOT_AVAILABLE if there are no free blocks at given tier
         */
        int removeAny (int tier, int lessThanBlock);
    }

//    /**
//     * Simple allocation tracker that uses multi-tier stack.
//     * This tracker allocates last-used blocks first, it is not "address ordered".
//     */
//    static final class IntStackTracker implements Tracker {
//
//        private final IntStack [] freelists;
//
//        IntStackTracker(int maxNumberOfTiersToKeep) {
//
//            freelists = new IntStack[maxNumberOfTiersToKeep];
//            for (int i=0; i < maxNumberOfTiersToKeep; i++)
//                freelists[i] = new IntStack(1 << (maxNumberOfTiersToKeep - i - 1));
//
//            add(maxNumberOfTiersToKeep-1, 0); // mark top tier block as free
//        }
//
//        @Override
//        public int getNumberOfTiers() {
//            return freelists.length;
//        }
//
//        @Override
//        public void add(int tier, int block) {
//            freelists[tier].push(block);
//        }
//
//        @Override
//        public boolean remove(int tier, int block) {
//            return freelists[tier].remove(block);
//        }
//
//        @Override
//        public int removeAny(int tier, int lessThanBlock) {
//            assert (lessThanBlock != NOT_AVAILABLE);
//            if (freelists[tier].size() > 0)
//                return freelists[tier].pop();
//            return NOT_AVAILABLE;
//        }
//
//        @Override
//        public String toString() {
//            StringBuilder sb = new StringBuilder();
//
//            for (int i=0; i < freelists.length; i++) {
//                if (i > 0)
//                    sb.append(' ');
//                sb.append(i);
//                sb.append(':');
//                sb.append(freelists[i]);
//            }
//            return sb.toString();
//        }
//    }

    /** Tracker uses BitSet to allocate "address ordered" best fit buddies (lowest addresses used first). */
    static final class BitSetTracker implements Tracker {
        private int maxNumberOfTiersToKeep;

        /** Bitmask of every tier, starting from tier 0  which has block size equal to 1 .. up until top tier that has only one block of size ( 1 << tier) */
        private final BitSet bitmask;

        BitSetTracker (int maxNumberOfTiersToKeep) {
            if (maxNumberOfTiersToKeep > 30)
                throw new IllegalArgumentException("Too many blocks to track");

            this.maxNumberOfTiersToKeep = maxNumberOfTiersToKeep;

            int numberOfBlocksToTrack = (1 << (maxNumberOfTiersToKeep)) - 1;
            bitmask = new BitSet(numberOfBlocksToTrack);

            // mark largest block size as available
            bitmask.set(numberOfBlocksToTrack - 1);
        }

        @Override
        public int getNumberOfTiers() {
            return maxNumberOfTiersToKeep;
        }

        @Override
        public void add(int tier, int block) {
            final int blockOffset = getBlockOffset(tier, block);
            if (ASSERTIONS_ENABLED)
                assert ! bitmask.get(blockOffset) : "unused block " + block + " at tier " + tier + " state:" + toString();
            bitmask.set(blockOffset);
        }

        @Override
        public boolean remove(int tier, int block) {
            final int blockOffset = getBlockOffset(tier, block);
            if (bitmask.get(blockOffset)) {
                bitmask.clear(blockOffset);
                return true;
            }
            return false;
        }

        @Override
        public int removeAny(int tier, int lessThanBlock) {
            final int tierOffset = getTierOffset(tier);
            final int numberOfTierBlocks = 1 << (maxNumberOfTiersToKeep - tier - 1);
            final int lessThanOffset = (lessThanBlock != NOT_AVAILABLE) ? getBlockOffset(tier, lessThanBlock) : Integer.MAX_VALUE;
            final int tierEndOffset = Math.min(tierOffset + numberOfTierBlocks, lessThanOffset);
            for (int j = tierOffset; j < tierEndOffset; j++) {
                if (bitmask.get(j)) {
                    bitmask.clear(j);
                    return (1 << tier) * (j - tierOffset);
                }
            }
            return NOT_AVAILABLE;
        }

        /** @return Dump of tracker state in the following format:    tier-number:[free-block1,free-block2,..] */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            int tierOffset = 0;
            for (int tier=0; tier < maxNumberOfTiersToKeep; tier++) {
                if (tier > 0)
                    sb.append(' ');
                sb.append(tier);
                sb.append(':');
                sb.append('[');
                int numberOfTierBlocks = 1 << (maxNumberOfTiersToKeep - tier - 1);

                boolean needComma = false;
                for (int i=0; i < numberOfTierBlocks; i++) {
                    if (bitmask.get(tierOffset + i)) {
                        if (needComma)
                            sb.append(',');
                        else
                            needComma = true;
                        sb.append((1 << tier) * i);
                    }
                }
                tierOffset += numberOfTierBlocks;
                sb.append(']');
            }

            return sb.toString();
        }

        private int getBlockOffset(int tier, int block) {
            int blockSize = 1 << tier;
            return getTierOffset(tier) + (block / blockSize);
        }

        private int getTierOffset(int tier) {
            return (1 << maxNumberOfTiersToKeep) -  ( 1 << (maxNumberOfTiersToKeep - tier));
        }

    }

}
