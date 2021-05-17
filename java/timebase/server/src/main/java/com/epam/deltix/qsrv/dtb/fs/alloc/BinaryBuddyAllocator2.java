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

/**
 * Extends BinaryBuddyAllocator with an ability to have minimum block size
 */
final class BinaryBuddyAllocator2 {

    public static final int NOT_AVAILABLE = BinaryBuddyAllocator.NOT_AVAILABLE;

    private final BinaryBuddyAllocator delegate;
    private final int shift;
    /**
     *
     * @param minBlockSize minimum allocation unit (you can request a smaller size but this implementation will return you a chunk of at least minBlockSize size)
     * @param maxBlockSize maximum allocation unit (equals to entire heap size this implementation manage).
     */
    BinaryBuddyAllocator2(int minBlockSize, int maxBlockSize)
    {
        if (minBlockSize >= maxBlockSize)
            throw new IllegalArgumentException();

        if (Integer.bitCount(minBlockSize) != 1)
            throw new IllegalArgumentException("minBlockSize must be a power of 2: " + minBlockSize);

        if (Integer.bitCount(maxBlockSize) != 1)
            throw new IllegalArgumentException("maxBlockSize must be a power of 2: " + maxBlockSize);

        int minTier = BinaryBuddyAllocator.getTierForSize(minBlockSize);
        int maxTier = BinaryBuddyAllocator.getTierForSize(maxBlockSize);

        int numberOfTiers = maxTier - minTier;
        if (numberOfTiers > 30)
            throw new IllegalArgumentException();

        shift = minTier;
        delegate = new BinaryBuddyAllocator(1 << numberOfTiers);
    }

    int allocate (int size) {
        int result = delegate.allocate(getBlockSize(size));
        if (result != BinaryBuddyAllocator.NOT_AVAILABLE)
            result = result << shift;

        //LOG.level(Level.INFO).append("(C) Alloc (").append(size).append(") = ").append(result).commit();
        return result;
    }

    void deallocate(int block, int size) {
        //LOG.level(Level.INFO).append("(C) Dealloc (").append(block).append(", ").append(block).append(')').commit();
        delegate.deallocate(block >> shift, getBlockSize(size));
    }

    int defragment(int block, int size) {
        int result = delegate.defragment(block >> shift, getBlockSize(size));
        if (result != BinaryBuddyAllocator.NOT_AVAILABLE)
            result = result << shift;
        return result;
    }

    private int getBlockSize(int size) {
        int result = 1 << shift;
        while (result < size)
            result = result << 1;
        return result >> shift;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
