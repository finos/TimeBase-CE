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
package com.epam.deltix.qsrv.dtb.fs.alloc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by Andy on 9/12/2015.
 */
public class Test_BinaryBuddyAllocation2 {

    private BinaryBuddyAllocator2 alloc = new BinaryBuddyAllocator2 (32, 128); // 32, 64, 128 = 3 tiers

    @Test
    public void allocFullHeapSize () {
        assertState("0:[] 1:[] 2:[0]");
        int block = alloc.allocate(128); // entire heap size
        assertEquals(0, block);
        assertState("0:[] 1:[] 2:[]");
        alloc.deallocate(block, 128);
        assertState("0:[] 1:[] 2:[0]");
    }

    @Test
    public void allocHalfHeapSize () {
        assertState("0:[] 1:[] 2:[0]");

        int block1 = alloc.allocate(64); // half of heap size
        assertEquals(0, block1);
        assertState("0:[] 1:[2] 2:[]");

        int block2 = alloc.allocate(64); // half of heap size
        assertEquals(64, block2);
        assertState("0:[] 1:[] 2:[]");

        alloc.deallocate(block1, 64);
        assertState("0:[] 1:[0] 2:[]");

        alloc.deallocate(block2, 64);
        assertState("0:[] 1:[] 2:[0]");
    }

    @Test
    public void allocSmallBlock () {
        assertState("0:[] 1:[] 2:[0]");

        int blockSize = 3;
        int block = alloc.allocate(blockSize);
        assertEquals(0, block);
        assertState("0:[1] 1:[2] 2:[]");

        alloc.deallocate(block, blockSize);
        assertState("0:[] 1:[] 2:[0]");
    }

    @Test
    public void allocSeveralSmallestBlocks () {
        assertState("0:[] 1:[] 2:[0]");

        final int blockSize = 3;

        int block1 = alloc.allocate(blockSize);
        assertEquals(0, block1);
        assertState("0:[1] 1:[2] 2:[]");

        int block2 = alloc.allocate(blockSize);
        assertEquals(32, block2);
        assertState("0:[] 1:[2] 2:[]");

        int block3 = alloc.allocate(blockSize);
        assertEquals(64, block3);
        assertState("0:[3] 1:[] 2:[]");

        int block4 = alloc.allocate(blockSize);
        assertEquals(96, block4);
        assertState("0:[] 1:[] 2:[]");

        alloc.deallocate(block1, blockSize);
        alloc.deallocate(block4, blockSize);
        alloc.deallocate(block2, blockSize);
        alloc.deallocate(block3, blockSize);
        assertState("0:[] 1:[] 2:[0]");
    }

    private void assertState (String expected) {
        assertEquals(expected, alloc.toString());
    }
}