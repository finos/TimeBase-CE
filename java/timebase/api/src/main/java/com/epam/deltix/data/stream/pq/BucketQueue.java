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
package com.epam.deltix.data.stream.pq;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Priority queue based on circular buffer of fixed size.
 * Effective for the case when keys are within relatively low fixed range: {@code maxKey - minKey < bucketCount}.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class BucketQueue<T> {
    private static final byte NO_VALUE = 0;
    private static final byte SINGLE_VALUE = 1;
    private static final byte MULTIPLE_VALUES = 2;

    private final int maxDequePoolSize;
    private final byte direction;

    private int size = 0; // Total size (including values in backlog)
    private final int bucketCount;
    private final byte[] buckets;
    private final Object[] values;

    private int headIndex = 0;
    private long headKey = Long.MIN_VALUE;

    private final PriorityQueueExt<T> backlog = new PriorityQueueExt<>(16, true);
    private final ArrayDeque<ArrayDeque> dequePool = new ArrayDeque<>();


    public BucketQueue(int bucketCount, boolean ascending) {
        this.bucketCount = bucketCount;
        this.buckets = new byte[bucketCount];
        this.values = new Object[bucketCount];
        this.maxDequePoolSize = (int) (Math.sqrt(bucketCount) + 1);
        this.direction = ascending ? (byte)1 : -1;
        Arrays.fill(buckets, NO_VALUE);
    }

    public void offer(T obj, long key) {
        key = key * direction; // In descending queue values have negated values.
        if (size == 0) {
            headKey = key;
        }

        size += 1;
        long headKeyDiff = key - headKey;
        if (headKeyDiff < 0) {
            addWithRollback(-headKeyDiff, key, obj);
            return;
        }
        if (headKeyDiff >= bucketCount) {
            // Too high value.
            backlog.offer(obj, key);
        } else {
            addInternalByDiff(obj, (int) headKeyDiff);
        }
    }

    private void addWithRollback(long diff, long newKey, T newObj) {
        assert diff > 0;
        int bucketsToDump = diff >= bucketCount ? bucketCount : (int) diff;
        dumpBucketsToBacklog(bucketsToDump);
        headKey = newKey;
        headIndex = wrapIndex(headIndex - bucketsToDump);
        addInternalByDiff(newObj, 0);
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

    private void dumpBucketsToBacklog(int bucketsToDump) {
        for (int i = -bucketsToDump; i < 0; i++) {
            int keyIndex = wrapIndex(headIndex + i);
            byte bucketValue = buckets[keyIndex];
            long key = headKey + i;
            switch (bucketValue) {
                case NO_VALUE: {
                    // Cell empty
                    break;
                }
                case SINGLE_VALUE: {
                    // One value was there
                    Object prevObj = values[keyIndex];
                    backlog.offer((T) prevObj, key);
                    buckets[keyIndex] = NO_VALUE;
                    values[keyIndex] = null;
                    break;
                }
                case MULTIPLE_VALUES: {
                    // Multiple values
                    ArrayDeque deque = (ArrayDeque) values[keyIndex];
                    while (true){
                        Object obj = deque.poll();
                        if (obj == null) {
                            break;
                        }
                        backlog.offer((T) obj, key);
                    }
                    buckets[keyIndex] = NO_VALUE;
                    values[keyIndex] = null;
                    returnDequeToPool(deque);
                    break;
                }
            }
        }
    }


    private void addInternalByDiff(T obj, int headKeyDiff) {
        int keyIndex = wrapIndex(headIndex + headKeyDiff);
        byte bucketValue = buckets[keyIndex];
        switch (bucketValue) {
            case NO_VALUE: {
                // Cell empty
                values[keyIndex] = obj;
                buckets[keyIndex] = SINGLE_VALUE;
                break;
            }
            case SINGLE_VALUE: {
                // One value was there
                ArrayDeque deque = getDequeFromPool();
                Object prevObj = values[keyIndex];
                deque.add(prevObj);
                deque.add(obj);
                values[keyIndex] = deque;
                buckets[keyIndex] = MULTIPLE_VALUES;
                break;
            }
            case MULTIPLE_VALUES: {
                // Multiple values
                ArrayDeque deque = (ArrayDeque) values[keyIndex];
                deque.add(obj);
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
            case MULTIPLE_VALUES:
            default: {
                // Multiple values
                ArrayDeque deque = (ArrayDeque) values[headIndex];
                result = deque.poll();
                updateBucketAfterDequeElementRemoval(deque, headIndex);
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
            int keyIndex = wrapIndex(headIndex + i);
            long newHeadKey = headKey + i;
            if (newHeadKey == minValueInBacklog) {
                // We reached backlog
                headIndex = keyIndex;
                headKey = newHeadKey;
                pullInElementsFromBacklog();
                assert buckets[keyIndex] != NO_VALUE;
                return;
            }
            if (buckets[keyIndex] != NO_VALUE) {
                headIndex = keyIndex;
                headKey = newHeadKey;
                return;
            }
        }
        throw new IllegalStateException();
    }

    private void initBucketsFromBacklog() {
        assert !backlog.isEmpty();
        headIndex = 0;

        long firstKey = backlog.peekKey();
        T firstValue = backlog.poll();
        headKey = firstKey;
        addInternalByDiff(firstValue, 0);
        pullInElementsFromBacklog();
    }

    private void pullInElementsFromBacklog() {
        while (!backlog.isEmpty()) {
            long nextKey = backlog.peekKey();
            long headKeyDiff = nextKey - headKey;
            if (headKeyDiff >= bucketCount) {
                // This key is too far. Stop.
                return;
            }
            T next = backlog.poll();
            addInternalByDiff(next, (int) headKeyDiff);
        }
    }

    private ArrayDeque getDequeFromPool() {
        ArrayDeque result = dequePool.poll();
        if (result == null) {
            return new ArrayDeque(4);
        } else {
            return result;
        }
    }

    private void returnDequeToPool(ArrayDeque deque) {
        if (dequePool.size() < maxDequePoolSize) {
            dequePool.add(deque);
        }
    }

    public boolean isEmpty() {
        return size == 0;
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
                case MULTIPLE_VALUES: {
                    // Multiple values
                    ArrayDeque deque = (ArrayDeque) values[bucketIndex];
                    boolean removed = deque.remove(obj);
                    if (removed) {
                        updateBucketAfterDequeElementRemoval(deque, bucketIndex);
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