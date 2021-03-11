package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * This class generates indexes for {@link TSRoot} in {@link PDStream}.
 *
 * Main {@link TSRoot} always should get index {@link #INDEX_FOR_ROOT}.
 *
 * All other {@link TSRoot}s get indexes according to {@link TSRoot#getSpace()} value.
 * Order of assigned indexes matches lexicographical order of space name.
 * So <pre> signum(String.compare(r1.getSpace(), r2.getSpace()) == signum(Integer.compare(r1.getSpaceIndex(), r2.getSpaceIndex()) </pre>
 * This means that sorting by {@link TSRoot#getSpaceIndex()} produces same order as sorting by {@link TSRoot#getSpace()}
 *
 * @author Alexei Osipov
 */
public class PDStreamSpaceIndexManager {
    private static final int INDEX_FOR_ROOT = 0; // Note: Current implementation does not permits negative indexes bcause othersie the range will
    private static final int MAX_INDEX = Integer.MAX_VALUE;

    public static final int NO_INDEX = Integer.MIN_VALUE;

    /**
     * Additional roots. Main root is not stored here and always has index {@link #INDEX_FOR_ROOT}.
     */
    private final ArrayList<TSRoot> roots = new ArrayList<>();

    private static final Comparator<TSRoot> COMPARATOR = (o1, o2) -> {
        String s1 = o1.getSpace();
        String s2 = o2.getSpace();
        assert s1 != null;
        assert s2 != null;

        return s1.compareTo(s2);
    };

    /**
     * Discards previously assigned indexes, cleans old root list and assigns new indexes considering only provided roots.
     *
     * @param defaultRoot main steam root: {@link PDStream#root}
     * @param additional all additional roots: {@link PDStream#roots}
     */
    public void resetAll(TSRoot defaultRoot, List<TSRoot> additional) {
        assert defaultRoot.getSpace() == null || defaultRoot.getSpace().equals("");
        clear();

        assert roots.isEmpty();
        roots.addAll(additional);
        roots.sort(COMPARATOR);

        defaultRoot.setSpaceIndex(INDEX_FOR_ROOT);

        int range = MAX_INDEX - INDEX_FOR_ROOT;
        int rangePerPart = range / (additional.size() + 1);
        if (rangePerPart < 1) {
            throw new IllegalArgumentException("Insufficient range capacity");
        }

        int lastUsedIndex = INDEX_FOR_ROOT;
        for (TSRoot tsr : roots) {
            if (tsr.getSpace() == null) {
                throw new IllegalArgumentException("space is not set");
            }
            lastUsedIndex += rangePerPart;
            tsr.setSpaceIndex(lastUsedIndex);
        }
    }

    /**
     * Assigns an index for a new root.
     *
     * @param x
     */
    public void addNew(TSRoot x) {
        if (x.getSpace() == null) {
            throw new IllegalArgumentException("space is not set");
        }

        int low = INDEX_FOR_ROOT;
        int size = roots.size();
        for (int i = 0; i < size; i++) {
            TSRoot tsr = roots.get(i);
            int c = COMPARATOR.compare(x, tsr);
            if (c == 0) {
                throw new IllegalArgumentException("Spaces can't be same. Duplicate space: " + x.getSpace());
            }
            int currentSpaceIndex = tsr.getSpaceIndex();
            if (c < 0) {
                // We found insertion point
                int range = currentSpaceIndex - low;
                insert(x, low, i, range);
                return;
            }
            low = currentSpaceIndex;
        }

        // If we got here then new element has highest value of "space" and must be added to the end
        int range = MAX_INDEX - low;
        insert(x, low, size, range);
    }

    private void insert(TSRoot x, int low, int insertionPos, int range) {
        if (range < 2) {
            throw new IllegalArgumentException("Insufficient range capacity");
        }
        int newIndex = range / 2 + low;
        x.setSpaceIndex(newIndex);
        roots.add(insertionPos, x);
    }

    /**
     * Clears list of roots (and resets indexes
     */
    public void clear() {
        for (TSRoot root : roots) {
            // Reset index for previously assigned roots
            root.setSpaceIndex(NO_INDEX);
        }
        roots.clear();
    }
}
