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