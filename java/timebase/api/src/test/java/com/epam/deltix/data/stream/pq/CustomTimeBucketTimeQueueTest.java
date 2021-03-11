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