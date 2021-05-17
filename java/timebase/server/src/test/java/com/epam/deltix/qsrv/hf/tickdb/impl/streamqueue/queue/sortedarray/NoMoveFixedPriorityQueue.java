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
package com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.queue.sortedarray;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.utilityclasses.MessageSourceComparator;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Alexei Osipov
 */
public class NoMoveFixedPriorityQueue<T extends TimeStampedMessage> {
    private final long[] timestamps;
    private final int[] indexes;
    private final MessageSource<T>[] sources;
    private final int size;

    private boolean headTaken = false;

    public NoMoveFixedPriorityQueue(Collection<MessageSource<T>> sources) {
        this.size = sources.size();

        List<MessageSource<T>> sourceList = new ArrayList<>(sources);
        sourceList.sort(new MessageSourceComparator<>());
        this.sources = sourceList.toArray(new MessageSource[size]);
        this.indexes = new int[size];
        this.timestamps = new long[size];
        ArrayList<MessageSource<T>> tmp = new ArrayList<>(sources);
        for (int i = 0; i < size; i++) {
            this.timestamps[i] = tmp.get(i).getMessage().getNanoTime();
            this.indexes[i] = i;
        }
    }

    public MessageSource<T> poll() {
        if (headTaken) {
            // Can't poll two times in a row
            throw new IllegalStateException();
        }

        headTaken = true;
        return sources[this.indexes[0]];
    }

    public void offer(MessageSource<T> obj, long newNanoTime) {
        if (!headTaken) {
            throw new IllegalStateException();
        }

        //MessageSource<T>[] sources = this.sources;
        int[] indexes = this.indexes;
        int objIndex = indexes[0];
        //assert obj == sources[objIndex];
        headTaken = false;

        if (size == 1) {
            // TODO: Extract case with single element "queue"
            return;
        }

        long[] timestamps = this.timestamps;
        if (newNanoTime <= timestamps[1]) {
            // We don't need to move anything
            timestamps[0] = newNanoTime; // TODO: Do we really need to update it? Probably we don't need to care about it.
        } else {
            // We have to place our head to a new position somewhere in the middle
            insertWithShift(newNanoTime, timestamps, indexes, objIndex, size);
        }
    }

    private static void insertWithShift(long newNanoTime, long[] timestamps, int[] indexes, int objIndex, int size) {
        int insertionPosition = Math.abs(insertionPositionBinarySearch(newNanoTime, size, timestamps));
        if (insertionPosition == size) {
            // Insert "after" last element
            insertionPosition = size - 1;
        }
        System.arraycopy(timestamps, 1, timestamps, 0, insertionPosition); // Shift element to left by 1
        System.arraycopy(indexes, 1, indexes, 0, insertionPosition);
        timestamps[insertionPosition] = newNanoTime;
        indexes[insertionPosition] = objIndex;
    }

    private static int insertionPositionBinarySearch(long key, int size, long[] a) {
        int low = 2; // We skip comparison with position 0 because it's "empty". We skip comparison with position 1 because we already compared to it.
        int high = size - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = a[mid];

            if (midVal < key)
                low = mid + 1;
            else if (midVal > key)
                high = mid - 1;
            else
                return mid - 1; // key found. -1 because we can avoid shifting this element
        }
        return low - 1;  // key not found.
    }
}
