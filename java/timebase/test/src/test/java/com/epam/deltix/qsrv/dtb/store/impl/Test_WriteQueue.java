/*
 * Copyright 2026 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.store.pub.TSRoot;
import com.epam.deltix.util.JUnitCategories;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

@Category(JUnitCategories.TickDBFast.class)
public class Test_WriteQueue {

    private WriteQueue writeQueue;
    private TSRoot root1;
    private TSRoot root2;
    private TSRoot root3;

    @Before
    public void setUp() {
        writeQueue = new WriteQueue();
        root1 = createRoot("root1");
        root2 = createRoot("root2");
        root3 = createRoot("root3");
    }

    private TSRoot createRoot(String name) {
        TSRoot mock = Mockito.mock(TSRoot.class);
        Mockito.when(mock.getSpace()).thenReturn(name); // Just to allow identification
        return mock;
    }

    private TSFile createFile(TSRoot root, long allocatedSize) {
        TSFile file = Mockito.mock(TSFile.class);
        Mockito.when(file.getRoot()).thenReturn(root);

        // Assuming TSFile has a way to set allocated size for testing
        Mockito.when(file.getAllocatedSize(Mockito.anyBoolean())).thenReturn(allocatedSize);
        return file;
    }

    // ==================== Basic Add/Poll Tests ====================

    @Test
    public void testAddAndPollSingleFile() throws InterruptedException {
        TSFile file = createFile(root1, 100);

        writeQueue.add(file);

        assertTrue(writeQueue.contains(file));
        assertEquals(100, writeQueue.getUsedTotalMemory());
        assertTrue(file.queued);

        WriteQueue.FileTask task = writeQueue.pollTask();

        assertSame(file, task.file);
        assertEquals(100, task.allocatedSize);
        assertFalse(task.badFile);
    }

    @Test
    public void testAddFailedFile() throws InterruptedException {
        TSFile file = createFile(root1, 200);

        writeQueue.addFailed(file);

        assertTrue(writeQueue.contains(file));
        assertEquals(200, writeQueue.getUsedTotalMemory());

        WriteQueue.FileTask task = writeQueue.pollTask();

        assertSame(file, task.file);
        assertTrue(task.badFile);
    }

    @Test
    public void testAddMultipleFilesFromDifferentRoots() throws InterruptedException {
        TSFile file1 = createFile(root1, 100);
        TSFile file2 = createFile(root2, 200);
        TSFile file3 = createFile(root3, 300);

        writeQueue.add(file1);
        writeQueue.add(file2);
        writeQueue.add(file3);

        assertEquals(600, writeQueue.getUsedTotalMemory());
        assertTrue(writeQueue.contains(file1));
        assertTrue(writeQueue.contains(file2));
        assertTrue(writeQueue.contains(file3));

        // Files should be polled in order of addition (by taskId)
        WriteQueue.FileTask task1 = writeQueue.pollTask();
        WriteQueue.FileTask task2 = writeQueue.pollTask();
        WriteQueue.FileTask task3 = writeQueue.pollTask();

        assertSame(file1, task1.file);
        assertSame(file2, task2.file);
        assertSame(file3, task3.file);
    }

    // ==================== Backlog Tests ====================

    @Test
    public void testAddMultipleFilesFromSameRoot_SecondGoesToBacklog() throws InterruptedException {
        TSFile file1 = createFile(root1, 100);
        TSFile file2 = createFile(root1, 200);

        writeQueue.add(file1);
        writeQueue.add(file2);

        assertEquals(300, writeQueue.getUsedTotalMemory());
        assertTrue(writeQueue.contains(file1));
        assertTrue(writeQueue.contains(file2));

        // First poll should return file1
        WriteQueue.FileTask task1 = writeQueue.pollTask();
        assertSame(file1, task1.file);

        // Report completion to allow next file from backlog
        writeQueue.reportTaskCompleted(task1);

        // Now file2 should be available
        WriteQueue.FileTask task2 = writeQueue.pollTask();
        assertSame(file2, task2.file);
    }

    @Test
    public void testBacklogOrdersTasksByTaskId() throws InterruptedException {
        TSFile file1 = createFile(root1, 100);
        TSFile file2 = createFile(root1, 200);
        TSFile file3 = createFile(root1, 300);

        writeQueue.add(file1);
        writeQueue.add(file2);
        writeQueue.add(file3);

        WriteQueue.FileTask task1 = writeQueue.pollTask();
        assertSame(file1, task1.file);
        writeQueue.reportTaskCompleted(task1);

        WriteQueue.FileTask task2 = writeQueue.pollTask();
        assertSame(file2, task2.file);
        writeQueue.reportTaskCompleted(task2);

        WriteQueue.FileTask task3 = writeQueue.pollTask();
        assertSame(file3, task3.file);
        writeQueue.reportTaskCompleted(task3);
    }

    // ==================== Bad File Priority Tests ====================

    @Test
    public void testBadFilesHaveLowerPriority() throws InterruptedException {
        TSFile badFile = createFile(root1, 100);
        TSFile goodFile1 = createFile(root2, 200);
        TSFile goodFile2 = createFile(root3, 200);

        writeQueue.add(goodFile1);
        writeQueue.addFailed(badFile);
        writeQueue.add(goodFile2);

        // Good file should be polled first due to lower taskId
        WriteQueue.FileTask task1 = writeQueue.pollTask();
        assertSame(goodFile1, task1.file);

        WriteQueue.FileTask task2 = writeQueue.pollTask();
        assertSame(goodFile2, task2.file);

        WriteQueue.FileTask task3 = writeQueue.pollTask();
        assertSame(badFile, task3.file);
    }

    @Test
    public void testGoodFileReplaceBadFileInMainQueue() throws InterruptedException {
        TSFile badFile = createFile(root1, 100);
        TSFile goodFile = createFile(root1, 200);

        // Add bad file first
        writeQueue.addFailed(badFile);
        // Then add good file for same root
        writeQueue.add(goodFile);

        // Good file should replace bad file in main queue
        WriteQueue.FileTask task1 = writeQueue.pollTask();
        assertSame(goodFile, task1.file);
        assertFalse(task1.badFile);

        // Report completion
        writeQueue.reportTaskCompleted(task1);

        // Bad file should be in backlog and polled next
        WriteQueue.FileTask task2 = writeQueue.pollTask();
        assertSame(badFile, task2.file);
        assertTrue(task2.badFile);
    }

    // ==================== Remove Tests ====================

    @Test
    public void testRemoveFromMainQueue() {
        TSFile file = createFile(root1, 100);

        writeQueue.add(file);
        assertTrue(writeQueue.contains(file));
        assertEquals(100, writeQueue.getUsedTotalMemory());

        boolean removed = writeQueue.remove(file);

        assertTrue(removed);
        assertFalse(writeQueue.contains(file));
        assertEquals(0, writeQueue.getUsedTotalMemory());
        assertFalse(file.queued);
    }

    @Test
    public void testRemoveFromBacklog() {
        TSFile file1 = createFile(root1, 100);
        TSFile file2 = createFile(root1, 200);

        writeQueue.add(file1);
        writeQueue.add(file2); // Goes to backlog

        assertEquals(300, writeQueue.getUsedTotalMemory());

        // Remove file2 from backlog
        boolean removed = writeQueue.remove(file2);

        assertTrue(removed);
        assertFalse(writeQueue.contains(file2));
        assertTrue(writeQueue.contains(file1));
        assertEquals(100, writeQueue.getUsedTotalMemory());
    }

    @Test
    public void testRemoveFromMainQueueMovesNextFromBacklog() throws InterruptedException {
        TSFile file1 = createFile(root1, 100);
        TSFile file2 = createFile(root1, 200);

        writeQueue.add(file1);
        writeQueue.add(file2);

        // Remove file1 from main queue
        boolean removed = writeQueue.remove(file1);

        assertTrue(removed);
        assertFalse(writeQueue.contains(file1));
        assertTrue(writeQueue.contains(file2));

        // file2 should now be in main queue and pollable
        WriteQueue.FileTask task = writeQueue.pollTask();
        assertSame(file2, task.file);
    }

    @Test
    public void testRemoveNonExistentFile() {
        TSFile file = createFile(root1, 100);

        boolean removed = writeQueue.remove(file);

        assertFalse(removed);
    }

    @Test
    public void testRemoveBadFileFromMainQueue() {
        TSFile badFile = createFile(root1, 100);

        writeQueue.addFailed(badFile);

        boolean removed = writeQueue.remove(badFile);

        assertTrue(removed);
        assertFalse(writeQueue.contains(badFile));
        assertEquals(0, writeQueue.getUsedTotalMemory());
    }

    @Test
    public void testRemoveRoot_RemovesMainAndBacklogTasksForRoot() throws InterruptedException {
        TSFile root1Main = createFile(root1, 100);
        TSFile root1Backlog = createFile(root1, 200);
        TSFile root2Main = createFile(root2, 300);

        writeQueue.add(root1Main);
        writeQueue.add(root1Backlog);
        writeQueue.add(root2Main);

        assertEquals(600, writeQueue.getUsedTotalMemory());
        assertEquals(600, writeQueue.getUsedQueueMemory());

        List<TSFile> removedFiles = writeQueue.removeRoot(root1);

        assertEquals(2, removedFiles.size());
        assertTrue(removedFiles.contains(root1Main));
        assertTrue(removedFiles.contains(root1Backlog));
        assertFalse(writeQueue.contains(root1Main));
        assertFalse(writeQueue.contains(root1Backlog));
        assertTrue(writeQueue.contains(root2Main));
        assertFalse(root1Main.queued);
        assertFalse(root1Backlog.queued);
        assertEquals(300, writeQueue.getUsedTotalMemory());
        assertEquals(300, writeQueue.getUsedQueueMemory());

        WriteQueue.FileTask remainingTask = writeQueue.pollTask();
        assertSame(root2Main, remainingTask.file);
    }

    @Test
    public void testRemoveRoot_WhileRootTaskIsProcessing_RemovesOnlyBacklog() throws InterruptedException {
        TSFile root1Processing = createFile(root1, 100);
        TSFile root1Backlog = createFile(root1, 200);

        writeQueue.add(root1Processing);
        writeQueue.add(root1Backlog);

        WriteQueue.FileTask processingTask = writeQueue.pollTask();
        assertSame(root1Processing, processingTask.file);
        assertEquals(300, writeQueue.getUsedTotalMemory());
        assertEquals(200, writeQueue.getUsedQueueMemory());

        List<TSFile> removedFiles = writeQueue.removeRoot(root1);

        assertEquals(1, removedFiles.size());
        assertSame(root1Backlog, removedFiles.get(0));
        assertFalse(writeQueue.contains(root1Backlog));
        assertFalse(root1Backlog.queued);
        assertEquals(100, writeQueue.getUsedTotalMemory());
        assertEquals(0, writeQueue.getUsedQueueMemory());

        writeQueue.reportTaskCompleted(processingTask);
        assertEquals(0, writeQueue.getUsedTotalMemory());
        assertEquals(0, writeQueue.getUsedQueueMemory());
    }

    @Test
    public void testRemoveRoot_WhenRootNotPresent_ReturnsEmptyList() {
        TSFile root1File = createFile(root1, 100);
        writeQueue.add(root1File);

        List<TSFile> removedFiles = writeQueue.removeRoot(root2);

        assertTrue(removedFiles.isEmpty());
        assertTrue(writeQueue.contains(root1File));
        assertEquals(100, writeQueue.getUsedTotalMemory());
        assertEquals(100, writeQueue.getUsedQueueMemory());
    }

    // ==================== Memory Tracking Tests ====================

    @Test
    public void testUsedMemoryAfterTaskCompletion() throws InterruptedException {
        TSFile file = createFile(root1, 500);

        writeQueue.add(file);
        assertEquals(500, writeQueue.getUsedTotalMemory());

        WriteQueue.FileTask task = writeQueue.pollTask();
        assertEquals(500, writeQueue.getUsedTotalMemory());

        writeQueue.reportTaskCompleted(task);
        assertEquals(0, writeQueue.getUsedTotalMemory());
    }

    @Test
    public void testUsedMemoryWithMultipleFiles() throws InterruptedException {
        TSFile file1 = createFile(root1, 100);
        TSFile file2 = createFile(root2, 200);

        writeQueue.add(file1);
        assertEquals(100, writeQueue.getUsedTotalMemory());

        writeQueue.add(file2);
        assertEquals(300, writeQueue.getUsedTotalMemory());

        WriteQueue.FileTask task1 = writeQueue.pollTask();
        writeQueue.reportTaskCompleted(task1);
        assertEquals(200, writeQueue.getUsedTotalMemory());

        WriteQueue.FileTask task2 = writeQueue.pollTask();
        writeQueue.reportTaskCompleted(task2);
        assertEquals(0, writeQueue.getUsedTotalMemory());
    }

    // ==================== Contains Tests ====================

    @Test
    public void testContainsAfterPoll() throws InterruptedException {
        TSFile file = createFile(root1, 100);

        writeQueue.add(file);
        assertTrue(writeQueue.contains(file));

        WriteQueue.FileTask task = writeQueue.pollTask();
        // After poll, file is no longer in either queue
        assertFalse(writeQueue.contains(file));
    }

    @Test
    public void testContainsFileInBacklog() {
        TSFile file1 = createFile(root1, 100);
        TSFile file2 = createFile(root1, 200);

        writeQueue.add(file1);
        writeQueue.add(file2);

        assertTrue(writeQueue.contains(file1));
        assertTrue(writeQueue.contains(file2));
    }

    // ==================== Blocking Behavior Tests ====================

    @Test(timeout = 5000)
    public void testPollTaskBlocksWhenQueueEmpty() throws InterruptedException {
        CountDownLatch pollStarted = new CountDownLatch(1);
        CountDownLatch taskReceived = new CountDownLatch(1);
        AtomicReference<WriteQueue.FileTask> receivedTask = new AtomicReference<>();

        Thread pollThread = new Thread(() -> {
            pollStarted.countDown();
            try {
                WriteQueue.FileTask task = writeQueue.pollTask();
                receivedTask.set(task);
                taskReceived.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        pollThread.start();
        pollStarted.await();

        // Give thread time to start waiting
        Thread.sleep(100);

        TSFile file = createFile(root1, 100);
        writeQueue.add(file);

        taskReceived.await();
        assertSame(file, receivedTask.get().file);

        pollThread.join();
    }

    // ==================== Concurrent Access Tests ====================

    @Test(timeout = 10000)
    public void testConcurrentAddAndPoll() throws InterruptedException {
        int numFiles = 1_000;
        List<TSFile> files = new ArrayList<>();
        List<TSRoot> roots = new ArrayList<>();

        // Create unique roots for each file to avoid backlog complexity
        for (int i = 0; i < numFiles; i++) {
            TSRoot root = createRoot("root" + i);
            roots.add(root);
            files.add(createFile(root, 10));
        }

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numFiles);
        List<WriteQueue.FileTask> polledTasks = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Add files
        for (TSFile file : files) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    writeQueue.add(file);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Poll tasks
        for (int i = 0; i < numFiles; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    WriteQueue.FileTask task = writeQueue.pollTask();
                    polledTasks.add(task);
                    writeQueue.reportTaskCompleted(task);
                    completeLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        completeLatch.await(10, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(numFiles, polledTasks.size());
        assertEquals(0, writeQueue.getUsedTotalMemory());
    }

    @Test(timeout = 10000)
    public void testConcurrentAddAndRemove() throws InterruptedException {
        int numFiles = 50;
        List<TSFile> files = new ArrayList<>();
        List<TSRoot> roots = new ArrayList<>();

        for (int i = 0; i < numFiles; i++) {
            TSRoot root = createRoot("root" + i);
            roots.add(root);
            files.add(createFile(root, 10));
        }

        // Add all files first
        for (TSFile file : files) {
            writeQueue.add(file);
        }

        assertEquals(numFiles * 10, writeQueue.getUsedTotalMemory());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(numFiles);

        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Remove files concurrently
        for (TSFile file : files) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    writeQueue.remove(file);
                    completeLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        completeLatch.await(10, TimeUnit.SECONDS);

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(0, writeQueue.getUsedTotalMemory());

        for (TSFile file : files) {
            assertFalse(writeQueue.contains(file));
        }
    }

    // ==================== Edge Cases ====================

    @Test
    public void testEmptyQueueContains() {
        TSFile file = createFile(root1, 100);
        assertFalse(writeQueue.contains(file));
    }

    @Test
    public void testInitialUsedMemoryIsZero() {
        assertEquals(0, writeQueue.getUsedTotalMemory());
    }

    @Test
    public void testAddGoodFileAfterBadFileForDifferentRoot() throws InterruptedException {
        TSFile badFile = createFile(root1, 100);
        TSFile goodFile = createFile(root2, 200);

        writeQueue.addFailed(badFile);
        writeQueue.add(goodFile);

        // Good file should be polled first
        WriteQueue.FileTask task1 = writeQueue.pollTask();
        assertSame(goodFile, task1.file);
        writeQueue.reportTaskCompleted(task1);

        WriteQueue.FileTask task2 = writeQueue.pollTask();
        assertSame(badFile, task2.file);
    }

    @Test
    public void testMultipleFilesFromSameRootWithMixedPriority() throws InterruptedException {
        TSFile goodFile1 = createFile(root1, 100);
        TSFile badFile1 = createFile(root1, 200);
        TSFile goodFile2 = createFile(root1, 300);

        writeQueue.add(goodFile1);
        writeQueue.addFailed(badFile1);
        writeQueue.add(goodFile2);

        // First file should be goodFile1
        WriteQueue.FileTask task1 = writeQueue.pollTask();
        assertSame(goodFile1, task1.file);
        writeQueue.reportTaskCompleted(task1);

        // Next should be goodFile2 (lower taskId than badFile1)
        WriteQueue.FileTask task2 = writeQueue.pollTask();
        assertSame(goodFile2, task2.file);
        writeQueue.reportTaskCompleted(task2);

        // Finally badFile1
        WriteQueue.FileTask task3 = writeQueue.pollTask();
        assertSame(badFile1, task3.file);
    }

    @Test
    public void testRemoveLastFileForRoot() throws InterruptedException {
        TSFile file1 = createFile(root1, 100);
        TSFile file2 = createFile(root1, 200);

        writeQueue.add(file1);
        writeQueue.add(file2);

        // Poll and complete first file
        WriteQueue.FileTask task1 = writeQueue.pollTask();
        writeQueue.reportTaskCompleted(task1);

        // Poll and complete second file
        WriteQueue.FileTask task2 = writeQueue.pollTask();
        writeQueue.reportTaskCompleted(task2);

        // Root should be completely cleared
        assertEquals(0, writeQueue.getUsedTotalMemory());
        assertFalse(writeQueue.contains(file1));
        assertFalse(writeQueue.contains(file2));
    }

    @Test
    public void testFileTaskProperties() {
        TSFile file = createFile(root1, 500);

        WriteQueue.FileTask task = new WriteQueue.FileTask(123L, file, 500L, true);

        assertEquals(123L, task.taskId);
        assertSame(file, task.file);
        assertEquals(500L, task.allocatedSize);
        assertTrue(task.badFile);
    }

    @Test
    public void testAddMultipleRootsWithBacklog() throws InterruptedException {
        TSFile file1Root1 = createFile(root1, 100);
        TSFile file2Root1 = createFile(root1, 200);
        TSFile file1Root2 = createFile(root2, 300);
        TSFile file2Root2 = createFile(root2, 400);

        writeQueue.add(file1Root1);
        writeQueue.add(file2Root1);
        writeQueue.add(file1Root2);
        writeQueue.add(file2Root2);

        assertEquals(1000, writeQueue.getUsedTotalMemory());

        // Poll file1Root1
        WriteQueue.FileTask task1 = writeQueue.pollTask();
        assertSame(file1Root1, task1.file);

        // Poll file1Root2 (next available from different root)
        WriteQueue.FileTask task2 = writeQueue.pollTask();
        assertSame(file1Root2, task2.file);

        // Complete task1, file2Root1 should become available
        writeQueue.reportTaskCompleted(task1);

        // Complete task2, file2Root2 should become available
        writeQueue.reportTaskCompleted(task2);

        // Poll remaining files
        WriteQueue.FileTask task3 = writeQueue.pollTask();
        WriteQueue.FileTask task4 = writeQueue.pollTask();

        // One should be file2Root1 and other file2Root2
        assertTrue(
                (task3.file == file2Root1 && task4.file == file2Root2) ||
                        (task3.file == file2Root2 && task4.file == file2Root1)
        );
    }

    @Test
    public void testQueuedFlagSetOnAdd() {
        TSFile file = createFile(root1, 100);
        assertFalse(file.queued);

        writeQueue.add(file);

        assertTrue(file.queued);
    }

    @Test
    public void testQueuedFlagClearedOnRemove() {
        TSFile file = createFile(root1, 100);

        writeQueue.add(file);
        assertTrue(file.queued);

        writeQueue.remove(file);
        assertFalse(file.queued);
    }

    @Test
    public void testRemoveFileNotInQueue() {
        TSFile file1 = createFile(root1, 100);
        TSFile file2 = createFile(root1, 200);

        writeQueue.add(file1);

        boolean removed = writeQueue.remove(file2);

        assertFalse(removed);
        assertTrue(writeQueue.contains(file1));
    }

    @Test(timeout = 5000)
    public void testPollNotifiesOnAdd() throws InterruptedException {
        Thread pollThread = new Thread(() -> {
            try {
                WriteQueue.FileTask task = writeQueue.pollTask();
                assertNotNull(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        pollThread.start();

        // Wait a bit for the thread to start waiting
        Thread.sleep(100);

        TSFile file = createFile(root1, 100);
        writeQueue.add(file);

        pollThread.join(3000);
        assertFalse(pollThread.isAlive());
    }

}
