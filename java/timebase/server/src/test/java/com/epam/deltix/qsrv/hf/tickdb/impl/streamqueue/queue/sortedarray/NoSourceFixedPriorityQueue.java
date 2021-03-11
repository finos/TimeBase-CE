package com.epam.deltix.qsrv.hf.tickdb.impl.streamqueue.queue.sortedarray;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.TimeStampedMessage;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Alexei Osipov
 */
public class NoSourceFixedPriorityQueue<T extends TimeStampedMessage> {
    private final long[] timestamps;
    private final long[] sources;
    private final int size;

    private boolean headTaken = false;

    public NoSourceFixedPriorityQueue(Collection<MessageSource<T>> sources) {
        this.size = sources.size();

        this.sources = new long[size];
        this.timestamps = new long[size];
        ArrayList<MessageSource<T>> tmp = new ArrayList<>(sources);
        for (int i = 0; i < size; i++) {
            this.timestamps[i] = tmp.get(i).getMessage().getNanoTime();
            this.sources[i] = tmp.get(i).getMessage().getNanoTime();
        }
    }

    public long poll() {
        if (headTaken) {
            // Can't poll two times in a row
            throw new IllegalStateException();
        }

        headTaken = true;
        return timestamps[0];
    }

    public void offer(long newNanoTime) {
        if (!headTaken) {
            throw new IllegalStateException();
        }

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
            insertWithShift(newNanoTime, newNanoTime, sources, timestamps);
        }
    }

    private void insertWithShift(long objToInsert, long newNanoTime, long[] sources, long[] timestamps) {
        int insertionPosition = Math.abs(insertionPositionBinarySearch(newNanoTime));
        if (insertionPosition == size) {
            // Insert "after" last element
            insertionPosition = size - 1;
        }
        System.arraycopy(timestamps, 1, timestamps, 0, insertionPosition); // Shift element to left by 1
        System.arraycopy(sources, 1, sources, 0, insertionPosition);
        timestamps[insertionPosition] = newNanoTime;
        sources[insertionPosition] = objToInsert;
    }

    private int insertionPositionBinarySearch(long key) {
        long[] a = this.timestamps;
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
