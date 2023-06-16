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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Arrays;
import java.io.PrintStream;

/**
 * Custom PriorityQueue impelemntation, which keeps order of the entries with the same timestamp
 */
public class TimestampPriorityQueue<T extends InstrumentMessage> extends AbstractQueue<T> {
    private Object[] elementData;
    private long[] timestamps; // performance trick: keep all timestamps together to hit L2 cache
    private int size;

    public TimestampPriorityQueue() {
        this(64);
    }

    public TimestampPriorityQueue(int capacity) {
        elementData = new Object[capacity];
        timestamps = new long[capacity];
        size = 0;
    }

    public Iterator<T> iterator() {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return size;
    }

    public boolean offer(T t) {
        ensureCapacity(size + 1);

        // find insertion point
        final long key = t.getTimeStampMs();
        int idx;
        if (size == 0) {
            idx = 0;
        } else {
            idx = Arrays.binarySearch(
                    timestamps,
                    0,
                    size,
                    key);
            if (idx >= 0) {
                // find the last msg with the same symbol
                for (int i = idx + 1; i < size; i++) {
                    if (timestamps[i] == key)
                        idx = i;
                    else
                        break;
                }
                idx++;
            } else
                idx = -idx - 1;
        }

        if (idx < size) {
            System.arraycopy(elementData, idx, elementData, idx + 1, size - idx);
            System.arraycopy(timestamps, idx, timestamps, idx + 1, size - idx);
        }
        size++;
        elementData[idx] = t;
        timestamps[idx] = key;
        return true;
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        if (size == 0)
            return null;

        // TODO: as optimisation I can just move start_position
        // and acually move the arrays, when a new element is inserted
        final T t = (T) elementData[0];
        size--;
        System.arraycopy(elementData, 1, elementData, 0, size);
        System.arraycopy(timestamps, 1, timestamps, 0, size);
        return t;
    }

    @SuppressWarnings("unchecked")
    public T peek() {
        if (size == 0)
            return null;
        else
            return (T) elementData[0];
    }

    public void ensureCapacity(int minCapacity) {
        int oldCapacity = elementData.length;
        if (minCapacity > oldCapacity) {
            int newCapacity = (oldCapacity * 3) / 2 + 1;
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;
            // minCapacity is usually close to size, so this is a win:
            elementData = Arrays.copyOf(elementData, newCapacity);
            timestamps = Arrays.copyOf(timestamps, newCapacity);
        }
    }

    @SuppressWarnings("unchecked")
    void dump(PrintStream ps) {
        for (Object o : elementData)
            ps.println(((T) o).getTimeStampMs());
    }
}