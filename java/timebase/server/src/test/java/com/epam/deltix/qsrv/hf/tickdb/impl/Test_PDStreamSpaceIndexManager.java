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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.google.common.base.Joiner;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.pub.EntityFilter;
import com.epam.deltix.qsrv.dtb.store.pub.PersistentDataStore;
import com.epam.deltix.qsrv.dtb.store.pub.SymbolRegistry;
import com.epam.deltix.qsrv.dtb.store.pub.TSRef;
import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.qsrv.dtb.store.pub.TimeRange;
import com.epam.deltix.qsrv.dtb.store.pub.TimeSliceIterator;
import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.util.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author Alexei Osipov
 */
public class Test_PDStreamSpaceIndexManager {

    @Test
    public void resetAll() {
        PDStreamSpaceIndexManager mgr = new PDStreamSpaceIndexManager();
        TestTSRoot a = new TestTSRoot("a");
        TestTSRoot b = new TestTSRoot("b");
        TestTSRoot c = new TestTSRoot("c");
        TestTSRoot d = new TestTSRoot("d");
        TSRoot[] tsRoots = {d, c, a, b};
        mgr.resetAll(new TestTSRoot(""), Arrays.asList(tsRoots));

        Assert.assertTrue(a.getSpaceIndex() < b.getSpaceIndex());
        Assert.assertTrue(b.getSpaceIndex() < c.getSpaceIndex());
        Assert.assertTrue(c.getSpaceIndex() < d.getSpaceIndex());
    }

    @Test
    public void addNew_sequentialOrder() {
        PDStreamSpaceIndexManager mgr = new PDStreamSpaceIndexManager();
        int prev = 0;
        List<TestTSRoot> roots = new ArrayList<>();

        for (int k = 1; k <= 31; k++) {
            String space = StringUtils.addLeadingZeros(Integer.toHexString(k), 2);
            TestTSRoot root = new TestTSRoot(space);
            mgr.addNew(root);
            Assert.assertTrue(root.getSpaceIndex() > prev);
            prev = root.getSpaceIndex();
            roots.add(root);
        }

        System.out.println("Indexes: " + Joiner.on(", ").join(roots.stream().map(TestTSRoot::getSpaceIndex).collect(Collectors.toList())));
    }

    @Test
    public void addNew_reverseOrder() {
        PDStreamSpaceIndexManager mgr = new PDStreamSpaceIndexManager();
        int prev = Integer.MAX_VALUE;
        List<TestTSRoot> roots = new ArrayList<>();

        for (int k = 30; k >= 1; k--) {
            String space = StringUtils.addLeadingZeros(Integer.toHexString(k), 2);
            TestTSRoot root = new TestTSRoot(space);
            mgr.addNew(root);
            Assert.assertTrue(root.getSpaceIndex() < prev);
            prev = root.getSpaceIndex();
            roots.add(root);
        }

        System.out.println("Indexes: " + Joiner.on(", ").join(roots.stream().map(TestTSRoot::getSpaceIndex).collect(Collectors.toList())));
    }

    @Test
    public void addNew_randomOrder() {
        PDStreamSpaceIndexManager mgr = new PDStreamSpaceIndexManager();

        List<TestTSRoot> roots = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            String space = StringUtils.addLeadingZeros(Integer.toHexString(i), 4);
            roots.add(new TestTSRoot(space));
        }


        List<TestTSRoot> shuffledRoots = new ArrayList<>(roots);
        Collections.shuffle(shuffledRoots, new Random(0));

        for (TestTSRoot root : shuffledRoots) {
            mgr.addNew(root);
        }

        shuffledRoots.sort(Comparator.comparingInt(TestTSRoot::getSpaceIndex));

        Assert.assertEquals(roots, shuffledRoots);

        //System.out.println("Indexes: " + Joiner.on(", ").join(roots.stream().map(TestTSRoot::getSpaceIndex).collect(Collectors.toList())));
    }

    private static class TestTSRoot implements TSRoot {
        private final String space;
        private int spaceIndex = Integer.MIN_VALUE;

        private TestTSRoot(String space) {
            this.space = space;
        }

        @Override
        public void setSpaceIndex(int index) {
            this.spaceIndex = index;
        }

        @Override
        public int getSpaceIndex() {
            return spaceIndex;
        }

        @Override
        public PersistentDataStore getStore() {
            return null;
        }

        @Override
        public void open(boolean readOnly) {

        }

        @Override
        public boolean isOpen() {
            return true;
        }

        @Override
        public void setSpace(String name) {

        }

        @Override
        public void format() {

        }

        @Override
        public void delete() {

        }

        @Override
        public SymbolRegistry getSymbolRegistry() {
            return null;
        }

        @Override
        public void setMaxFolderSize(int numTimeSlices) {

        }

        @Override
        public int getMaxFolderSize() {
            return 0;
        }

        @Override
        public void setMaxFileSize(int numBytes) {

        }

        @Override
        public int getMaxFileSize() {
            return 0;
        }

        @Override
        public String getCompression() {
            return null;
        }

        @Override
        public void setCompression(String compression) {

        }

        @Override
        public void getTimeRange(int id, TimeRange out) {

        }

        @Override
        public void getTimeRange(TimeRange out) {

        }

        @Override
        public AbstractFileSystem getFileSystem() {
            return null;
        }

        @Override
        public TimeInterval[] getTimeRanges(int[] ids) {
            return new TimeInterval[0];
        }

        @Override
        public void close() {

        }

        @Override
        public AbstractPath getPath() {
            return null;
        }

        @Override
        public void forceClose() {

        }

        @Override
        public void selectTimeSlices(TimeRange timeRange, EntityFilter filter, Collection<TSRef> addTo) {

        }

        @Override
        public String getPathString() {
            return null;
        }

        @Override
        public boolean drop(TimeRange range) {
            return false;
        }

        @Override
        public void iterate(TimeRange range, EntityFilter filter, TimeSliceIterator it) {

        }

        @Override
        public TSRef associate(String path) {
            return null;
        }

        @Nullable
        @Override
        public String getSpace() {
            return space;
        }

        @Override
        public String toString() {
            return "TestTSRoot{" +
                    "space='" + space + '\'' +
                    ", spaceIndex=" + spaceIndex +
                    '}';
        }
    }
}