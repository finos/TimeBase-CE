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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Alexei Osipov
 */
public class CustomTimeBucketTimeQueueTest {

    @Test
    public void testReverseAdd() throws Exception {
        int bucketCount = 1000;
        int iterations = 100_000;
        ArrayList<Integer> addedValues = new ArrayList<>(iterations * 6);
        RegularBucketQueue<Integer> queue = new RegularBucketQueue<>(bucketCount, 10, true);
        for (int i = 0; i < iterations; i++) {
            addToQueue(queue, addedValues, 1-i);
            addToQueue(queue, addedValues, 2-i);
            addToQueue(queue, addedValues, 3-i);
            addToQueue(queue, addedValues, 1-i);
            addToQueue(queue, addedValues, 2-i);
            addToQueue(queue, addedValues, 3-i);
        }

        Collections.sort(addedValues);
        for (int i = 0; i < addedValues.size(); i++) {
            Integer expected = addedValues.get(i);
            Integer valueFromQueue = queue.poll();
            if (!expected.equals(valueFromQueue)) {
                Assert.assertEquals(expected, valueFromQueue);
            }

        }
        Assert.assertTrue(queue.isEmpty());
    }

    private void addToQueue(RegularBucketQueue<Integer> queue, ArrayList<Integer> addedValues, int val) {
        Integer objVal = val;
        queue.offer(objVal, val);
        addedValues.add(objVal);
    }


    @Test
    public void testRemove() throws Exception {
        int bucketCount = 10;
        int iterations = 100;
        ArrayList<Integer> addedValues = new ArrayList<>(iterations * 6);
        RegularBucketQueue<Integer> queue = new RegularBucketQueue<>(bucketCount, 10, true);
        for (int i = 0; i < iterations; i++) {
            addToQueue(queue, addedValues, 1-i);
            addToQueue(queue, addedValues, 2-i);
            addToQueue(queue, addedValues, 3-i);
            addToQueue(queue, addedValues, 1-i);
            addToQueue(queue, addedValues, 2-i);
            addToQueue(queue, addedValues, 3-i);
        }

        Collections.sort(addedValues);
        for (int i = 0; i < addedValues.size(); i++) {
            Integer previouslyAdded = addedValues.get(i);
            boolean remove = queue.remove(previouslyAdded);
            Assert.assertTrue(remove);
        }
        Assert.assertTrue(queue.isEmpty());
    }

}