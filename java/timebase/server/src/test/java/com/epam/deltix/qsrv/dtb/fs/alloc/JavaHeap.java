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