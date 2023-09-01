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
package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.collections.generated.ObjectArrayList;

/**
 * Pool of objects of the same type. Thread-unsafe. Reset makes all allocated instances available again.
 */
public abstract class ObjectPool<T> {

    private final ObjectArrayList<T> pool;
    private int availIdx = 0;

    public ObjectPool() {
        this(5);
    }

    protected ObjectPool(int capacity) {
        pool = new ObjectArrayList<>(capacity);
    }

    public abstract T       newItem();

    public final T          borrow() {
        if (availIdx < pool.size()) {
            T item = pool.get(availIdx++);
            resetItem(item);
            return item;
        } else {
            availIdx = Integer.MAX_VALUE; // give up on reusage until reset
            final T item = newItem();
            pool.add(item);
            return item;
        }
    }

    public final void       reset() {
        availIdx = 0;
    }

    protected void resetItem(T item) {
    }
}