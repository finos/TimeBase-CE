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
package com.epam.deltix.data.stream.pq;

import com.google.common.annotations.VisibleForTesting;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Similar to {@link BucketQueue} but with customizable bucket size.
 * {@link RegularBucketQueue} with bucketSize=1 should behave like {@link BucketQueue}.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class RegularBucketQueue<T> {
    private static final byte NO_VALUE = 0;
    private static final byte SINGLE_VALUE = 1;
    private static final byte MULTIPLE_VALUES_SAME_KEYS = 2;
    private static final byte MULTIPLE_VALUES_WITH_ORDERING = 3;

    private final int maxDequePoolSize;
    private final byte direction;

    private int size = 0; // Total size (including values in backlog)
    private final int bucketCount;
    private final long bucketSize;
    private final byte[] buckets;
    private final Object[] values;
    private final long[] bucketKeys;

    private int headIndex = 0;
    private long headGlobalIndex = Long.MIN_VALUE;

    private final PriorityQueueExt<T> backlog = new PriorityQueueExt<>(16, true);
    private final ArrayDeque<ArrayDeque> dequePool = new ArrayDeque<>();
    private final ArrayDeque<PriorityQueueExt> orderedQueuePool = new ArrayDeque<>();
    private boolean mixedKeysDetected = false;

    public RegularBucketQueue(int bucketCount, long bucketSize, boolean ascending) {
        this.bucketCount = bucketCount;
        this.bucketSize = bucketSize;
        this.buckets = new byte[bucketCount];
        this.values = new Object[bucketCount];
        this.bucketKeys = new long[bucketCount];
        this.maxDequePoolSize = (int) (Math.sqrt(bucketCount) + 1);
        this.direction = ascending ? (byte)1 : -1;
        Arrays.fill(buckets, NO_VALUE);
    }

    public void offer(T obj, long key) {
        key = key * direction; // In descending queue values have negated values.
        long keyGlobalIndex = keyToGlobalIndex(key);
        if (size == 0) {
            headGlobalIndex = keyGlobalIndex;
        }

        size += 1;
        long headGlobalIndexDiff = keyGlobalIndex - headGlobalIndex;
        if (headGlobalIndexDiff < 0) {
            addWithRollback(-headGlobalIndexDiff, key, obj, keyGlobalIndex);
            return;
        }


        if (headGlobalIndexDiff >= bucketCount) {
            // Too high value.
            backlog.offer(obj, key);
        } else {
            addInternalByDiff(obj, (int) headGlobalIndexDiff, key);
        }
    }

    private void addWithRollback(long diff, long newKey, T newObj, long keyGlobalIndex) {
        assert diff > 0;
        int bucketsToDump = diff >= bucketCount ? bucketCount : (int) diff;
        dumpBucketsToBacklog(bucketsToDump);
        headGlobalIndex = keyGlobalIndex;
        headIndex = wrapIndex(headIndex - bucketsToDump);
        addInternalByDiff(newObj, 0, newKey);
    }

    /**
     * @return value in range [0, bucketCount). Note: input index is expected in range.
     */
    private int wrapIndex(int index) {
        // Right formula is Math.floorMod(index, bucketCount) but we use simplified version.
        // This version works fine if (index >= -bucketCount).
        // assert index >= bucketCount;
        return (index + bucketCount) % bucketCount;
    }

    private long keyToGlobalIndex(long key) {
        return key / bucketSize;
    }

    private long globalIndexToFirstKey(long offset) {
        return offset * bucketSize;
    }

    private void dumpBucketsToBacklog(int bucketsToDump) {
        for (int i = -bucketsToDump; i < 0; i++) {
            int keyIndex = wrapIndex(headIndex + i);
            byte bucketValue = buckets[keyIndex];
            //long key = headKey + i;
            switch (bucketValue) {
                case NO_VALUE: {
                    // Cell empty
                    break;
                }
                case SINGLE_VALUE: {
                    // One value was there
                    Object prevObj = values[keyIndex];
                    backlog.offer((T) prevObj, bucketKeys[keyIndex]);
                    buckets[keyIndex] = NO_VALUE;
                    values[keyIndex] = null;
                    break;
                }
                case MULTIPLE_VALUES_SAME_KEYS: {
                    // Multiple values
                    ArrayDeque deque = (ArrayDeque) values[keyIndex];
                    long bucketKey = bucketKeys[keyIndex];
                    while (true){
                        Object obj = deque.poll();
                        if (obj == null) {
                            break;
                        }
                        backlog.offer((T) obj, bucketKey);
                    }
                    buckets[keyIndex] = NO_VALUE;
                    values[keyIndex] = null;
                    returnDequeToPool(deque);
                    break;
                }
                case MULTIPLE_VALUES_WITH_ORDERING: {
                    // Multiple values
                    // TODO: Any way to efficiently merge trees?
                    PriorityQueueExt queue = (PriorityQueueExt) values[keyIndex];
                    while (!queue.isEmpty()){
                        long key = queue.peekKey();
                        Object obj = queue.poll();
                        if (obj == null) {
                            break;
                        }
                        backlog.offer((T) obj, key);
                    }
                    buckets[keyIndex] = NO_VALUE;
                    values[keyIndex] = null;
                    returnOrderedQueueToPool(queue);
                    break;
                }
            }
        }
    }

    private void addInternalByDiff(T obj, int headGlobalIndexDiff, long key) {
        int keyIndex = wrapIndex(headIndex + headGlobalIndexDiff);
        byte bucketValue = buckets[keyIndex];
        switch (bucketValue) {
            case NO_VALUE: {
                // Cell empty
                values[keyIndex] = obj;
                buckets[keyIndex] = SINGLE_VALUE;
                bucketKeys[keyIndex] = key;
                break;
            }
            case SINGLE_VALUE: {
                // One value was there
                long bucketKey = bucketKeys[keyIndex];
                if (!mixedKeysDetected) {
                    if (key != bucketKey) {
                        // New value does not matches key (multiple different keys for single bucket)
                        mixedKeysDetected = true;
                    }
                }
                if (mixedKeysDetected) {
                    PriorityQueueExt queue = getOrderedQueueFromPool();
                    Object prevObj = values[keyIndex];
                    queue.offer(prevObj, bucketKey);
                    queue.offer(obj, key);
                    values[keyIndex] = queue;
                    buckets[keyIndex] = MULTIPLE_VALUES_WITH_ORDERING;
                } else {
                    ArrayDeque deque = getDequeFromPool();
                    Object prevObj = values[keyIndex];
                    deque.add(prevObj);
                    deque.add(obj);
                    values[keyIndex] = deque;
                    buckets[keyIndex] = MULTIPLE_VALUES_SAME_KEYS;
                }
                break;
            }

            case MULTIPLE_VALUES_SAME_KEYS: {
                // Multiple values
                long bucketKey = bucketKeys[keyIndex];
                boolean keysMatch = key == bucketKey;
                ArrayDeque deque = (ArrayDeque) values[keyIndex];
                if (keysMatch) {
                    deque.add(obj);
                } else {
                    // Mismatch => we need to convert list to priority queue
                    PriorityQueueExt queue = getOrderedQueueFromPool();
                    while (true) {
                        Object oldObj = deque.poll();
                        if (oldObj == null) {
                            break;
                        }
                        queue.offer(oldObj, bucketKey);
                    }
                    values[keyIndex] = queue;
                    buckets[keyIndex] = MULTIPLE_VALUES_WITH_ORDERING;
                    returnDequeToPool(deque);
                }
                break;
            }
            case MULTIPLE_VALUES_WITH_ORDERING: {
                // Multiple values
                PriorityQueueExt queue = (PriorityQueueExt) values[keyIndex];
                queue.offer(obj, key);
                break;
            }
        }
    }

    @Nullable
    public T poll() {
        if (size == 0) {
            return null;
        }
        byte bucketValue = buckets[headIndex];
        if (bucketValue == NO_VALUE) {
            advanceHead();
            bucketValue = buckets[headIndex];
            assert bucketValue != NO_VALUE;
        }

        Object result;
        switch (bucketValue) {
            case NO_VALUE: {
                // Cell empty
                throw new IllegalStateException();
            }
            case SINGLE_VALUE: {
                // One value was there
                result = values[headIndex];
                updateBucketAfterSingleElementRemoval(headIndex);
                break;
            }
            case MULTIPLE_VALUES_SAME_KEYS: {
                // Multiple values
                ArrayDeque deque = (ArrayDeque) values[headIndex];
                result = deque.poll();
                updateBucketAfterDequeElementRemoval(deque, headIndex);
                break;
            }
            case MULTIPLE_VALUES_WITH_ORDERING:
            default: {
                // Multiple values
                PriorityQueueExt queue = (PriorityQueueExt) values[headIndex];
                result = queue.poll();
                updateBucketAfterOrderedQueueElementRemoval(queue, headIndex);
                break;
            }
        }
        return (T) result;
    }

    private void updateBucketAfterSingleElementRemoval(int bucketIndex) {
        size -= 1;
        values[bucketIndex] = null;
        buckets[bucketIndex] = NO_VALUE;
    }

    private void updateBucketAfterDequeElementRemoval(ArrayDeque deque, int bucketIndex) {
        size -= 1;
        int dequeSize = deque.size();
        assert dequeSize >= 1;
        if (dequeSize == 1) {
            // Only one element left
            buckets[bucketIndex] = SINGLE_VALUE;
            values[bucketIndex] = deque.poll();
            returnDequeToPool(deque);
        }
    }

    private void updateBucketAfterOrderedQueueElementRemoval(PriorityQueueExt queue, int bucketIndex) {
        size -= 1;
        int queueSize = queue.size();
        assert queueSize >= 1;
        if (queueSize == 1) {
            // Only one element left
            buckets[bucketIndex] = SINGLE_VALUE;
            values[bucketIndex] = queue.poll();
            returnOrderedQueueToPool(queue);
        }
    }

    private void advanceHead() {
        if (size == 0) {
            return;
        }
        if (size == backlog.size()) {
            // No data left in buckets. We have to fill in from queue.
            initBucketsFromBacklog();
            return;
        }

        long minValueInBacklog = backlog.isEmpty() ? Long.MAX_VALUE : backlog.peekKey();

        // We know that at least one element present in queue.
        for (int i = 1; i < bucketCount; i++) {
            int keyIndex = (headIndex + i) % bucketCount;
            long newHeadGlobalIndex = headGlobalIndex + i;

            long nextHeadKey = globalIndexToFirstKey(headGlobalIndex + i + 1);
            if (nextHeadKey > minValueInBacklog) {
                // We reached backlog (at least one element from backlog should be moved to this bucket)
                headIndex = keyIndex;
                headGlobalIndex = newHeadGlobalIndex;
                pullInElementsFromBacklog();
                assert buckets[keyIndex] != NO_VALUE;
                return;
            }
            if (buckets[keyIndex] != NO_VALUE) {
                headIndex = keyIndex;
                headGlobalIndex = newHeadGlobalIndex;
                return;
            }
        }
        throw new IllegalStateException();
    }

    /**
     * Special case: all buckets are empty and non-empty backlog.
     */
    private void initBucketsFromBacklog() {
        assert !backlog.isEmpty();
        headIndex = 0;

        long firstKey = backlog.peekKey();
        T firstValue = backlog.poll();
        headGlobalIndex = keyToGlobalIndex(firstKey);
        addInternalByDiff(firstValue, 0, firstKey);
        pullInElementsFromBacklog();
    }

    private void pullInElementsFromBacklog() {
        while (!backlog.isEmpty()) {
            long nextKey = backlog.peekKey();
            long keyGlobalIndex = keyToGlobalIndex(nextKey);
            long headGlobalIndexDiff = keyGlobalIndex - headGlobalIndex;
            if (headGlobalIndexDiff >= bucketCount) {
                // This key is too far. Stop.
                return;
            }
            T next = backlog.poll();
            addInternalByDiff(next, (int) headGlobalIndexDiff, nextKey);
        }
    }

    private ArrayDeque getDequeFromPool() {
        ArrayDeque result = dequePool.poll();
        if (result == null) {
            return new ArrayDeque(8);
        } else {
            return result;
        }
    }

    private void returnDequeToPool(ArrayDeque deque) {
        if (dequePool.size() < maxDequePoolSize && !mixedKeysDetected) {
            dequePool.add(deque);
        }
    }

    private PriorityQueueExt getOrderedQueueFromPool() {
        PriorityQueueExt result = orderedQueuePool.poll();
        if (result == null) {
            return new PriorityQueueExt(8, true);
        } else {
            return result;
        }
    }

    private void returnOrderedQueueToPool(PriorityQueueExt orderedQueue) {
        if (orderedQueuePool.size() < maxDequePoolSize) {
            orderedQueuePool.add(orderedQueue);
        }
    }

    public boolean isEmpty() {
        return size == 0;
    }

    @VisibleForTesting
    public boolean isMixedKeysDetected() {
        return mixedKeysDetected;
    }

    public boolean remove(T obj) {
        if (size == 0) {
            return false;
        }
        for (int bucketIndex = 0; bucketIndex < bucketCount; bucketIndex++) {
            byte bucketValue = buckets[bucketIndex];
            switch (bucketValue) {
                case SINGLE_VALUE: {
                    // One value was there
                    if (obj.equals(values[bucketIndex])) {
                        updateBucketAfterSingleElementRemoval(bucketIndex);
                        return true;
                    }
                    break;
                }
                case MULTIPLE_VALUES_SAME_KEYS: {
                    // Multiple values
                    ArrayDeque deque = (ArrayDeque) values[bucketIndex];
                    boolean removed = deque.remove(obj);
                    if (removed) {
                        updateBucketAfterDequeElementRemoval(deque, bucketIndex);
                        return true;
                    }
                    break;
                }
                case MULTIPLE_VALUES_WITH_ORDERING: {
                    // Multiple values
                    PriorityQueueExt queue = (PriorityQueueExt) values[bucketIndex];
                    boolean removed = queue.remove(obj);
                    if (removed) {
                        updateBucketAfterOrderedQueueElementRemoval(queue, bucketIndex);
                        return true;
                    }
                    break;
                }
            }
        }
        // We scanned buckets but not found the value. Let's check backlog.
        boolean removed = backlog.remove(obj);
        if (removed) {
            size -= 1;
        }
        return removed;
    }
}