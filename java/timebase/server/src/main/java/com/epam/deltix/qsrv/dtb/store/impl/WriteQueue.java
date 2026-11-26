/*
 * Copyright 2024 EPAM Systems, Inc
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
import com.epam.deltix.qsrv.hf.blocks.ObjectPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Queue for {@link TSFile} write tasks.
 * <p>
 * It ensures that at most one file per {@link TSRoot} can be processed at any time.
 * <p>
 * Guarantees task order for each root, i.e. files for the same root are processed in the order they were added.
 * The only exception is when a "bad" file (previously failed) is added - it always has lower priority than any other file.
 * <p>
 * Guarantees that "bad" files are taken for processing only when there are no "good" files available for processing.
 */
class WriteQueue {
    private static final long LOW_PRIORITY_PENALTY = Long.MAX_VALUE / 2;

    // Memory used by files in queue, not including files being currently processed.
    private final AtomicLong usedQueueMemory = new AtomicLong(0);
    // Memory used by files in queue and currently being processed. Always >= usedQueueMemory.
    private final AtomicLong usedTotalMemory = new AtomicLong(0);

    // Worker is considered idle if it's waiting for a task to be available in the queue.
    private final AtomicInteger idleWorkers = new AtomicInteger(0);

    private final Object lock = new Object();

    // guarded by "lock"
    // Monotonically increasing task ID generator.
    // Allows to have global task order even if we move tasks between multiple collections.
    private long lastTaskId = 0;

    // guarded by "lock"
    // Main task queue ordered by taskId.
    // Only one task per root can be present here at any time.
    private final TreeMap<Long, FileTask> taskQueue = new TreeMap<>();

    // guarded by "lock"
    // Queue for "backlog" tasks per root.
    // If a root in this map, then the file is used by a writer OR in "taskQueue".
    // Empty queue means that task for this root is currently scheduled or being processed.
    // When the last task for the root is completed, the root is removed from this map.
    // We use TreeMap here because we need to efficiently handle "bad" files, that must have lower priority than all other files.
    private final Map<TSRoot, TreeMap<Long, FileTask>> queueByRoot = new HashMap<>();

    // guarded by "lock"
    // If root is present here, it means that "taskQueue" contains a "bad" file for this root.
    // This collection is needed to handle special situation, when a "good" file is added while a "bad" file for the same root is already in the main queue.
    private final Set<TSRoot> badFileInMainQueue = new HashSet<>();

    // guarded by "lock"
    private final ObjectPool<TreeMap<Long, FileTask>> queuePool = new ObjectPool<>(0) {
        @Override
        protected TreeMap<Long, FileTask> newItem() {
            return new TreeMap<>();
        }
    };

    WriteQueue() {
    }

    void add(TSFile tsf) {
        addTask(tsf, false);
    }

    // Add low priority task (previously failed file)
    void addFailed(TSFile tsf) {
        addTask(tsf, true);
    }

    private void addTask(TSFile tsf, boolean badFile) {
        long allocatedSize = tsf.getAllocatedSize(true);

        synchronized (lock) {
            assert !contains(tsf) : tsf + " is already queued";

            usedTotalMemory.addAndGet(allocatedSize);
            usedQueueMemory.addAndGet(allocatedSize);
            long taskId = ++lastTaskId + (badFile ? LOW_PRIORITY_PENALTY : 0);
            FileTask newTask = new FileTask(taskId, tsf, allocatedSize, badFile);

            tsf.queued = true;

            if (PDSImpl.LOGGER.isDebugEnabled()) {
                PDSImpl.LOGGER.debug("Adding to the write queue: %s")
                        .with(tsf.getPathString());
            }

            TSRoot root = getRoot(tsf);

            var queueForRoot = queueByRoot.get(root);
            boolean newRoot = queueForRoot == null;
            if (newRoot) {
                // Add to backlog of tasks for this root
                queueForRoot = queuePool.borrow();
                // Empty queue works as a marker to indicate that this root is in use. Also, it will accumulate further tasks (if any)
                queueByRoot.put(root, queueForRoot);

                // Schedule immediately for next available writer
                taskQueue.put(taskId, newTask);
                if (badFile) {
                    badFileInMainQueue.add(root);
                }
                lock.notify();
            } else {
                // Existing root
                if (!badFile && badFileInMainQueue.contains(root)) {
                    // Special case:
                    //  New file is good, but there is already a bad file for this root in the main queue.
                    //  We have to remove the bad file from queue and schedule the good file instead.
                    FileTask badFileTask = findByRootInMainQueue(root);
                    assert badFileTask != null : "Bad file task not found in main queue for root " + root;
                    taskQueue.remove(badFileTask.taskId);
                    taskQueue.put(taskId, newTask); // Schedule good file task

                    queueForRoot.put(badFileTask.taskId, badFileTask); // Move bad file task to backlog

                    badFileInMainQueue.remove(root);
                } else {
                    // Just add to the backlog of tasks for this root
                    queueForRoot.put(taskId, newTask);
                }
            }
        }
    }

    /**
     * Warning: this method is relatively slow, as it have to perform linear search across values in maps.
     */
    boolean remove(TSFile tsf) {
        boolean removed;

        synchronized (lock) {
            TSRoot root = getRoot(tsf);
            TreeMap<Long, FileTask> queueForRoot = queueByRoot.get(root);
            if (queueForRoot == null) {
                return false;
            }

            // Search in queueByRoot
            FileTask foundTask = removeFromQueue(tsf, queueForRoot);
            // It may make queue for this root empty but we still have to keep it as a marker that this root is in use.

            // Search in taskQueue
            if (foundTask == null) {
                foundTask = removeFromQueue(tsf, taskQueue);
                if (foundTask != null) {
                    if (foundTask.badFile) {
                        badFileInMainQueue.remove(root);
                    }
                    moveNextTaskFromBacklog(root);
                }
            }

            removed = foundTask != null;

            if (removed) {
                tsf.queued = false;
                usedQueueMemory.addAndGet(-foundTask.allocatedSize);
                usedTotalMemory.addAndGet(-foundTask.allocatedSize);
            }
        }

        return removed;
    }

    /**
     * Removes all tasks for the given root from the queue.
     * <p>
     * Warning: this method is relatively slow, as it have to perform linear search across values in two collections.
     *
     * @return list of all removed files
     */
    List<TSFile> removeRoot(TSRoot root) {
        synchronized (lock) {
            TreeMap<Long, FileTask> queueForRoot = queueByRoot.get(root);
            if (queueForRoot == null) {
                // No tasks for this root
                return Collections.emptyList();
            }

            List<TSFile> removedFiles = new ArrayList<>(Math.max(queueForRoot.size(), 10));
            long removedByteSize = 0;

            for (FileTask task : queueForRoot.values()) {
                removedFiles.add(task.file);
                removedByteSize += task.allocatedSize;
                task.file.queued = false;
            }

            queueForRoot.clear();

            FileTask mainQueueTask = findByRootInMainQueue(root);

            // Important: If mainQueueTask is null, we can't remove the root from "queueByRoot"
            // and release the queue to the pool, because it is still used as a marker that this root is in use.
            if (mainQueueTask != null) {
                // This root is not being processed at the moment,
                // so we can just remove it from the main queue completely.
                queuePool.release(queueForRoot);
                queueByRoot.remove(root);

                taskQueue.remove(mainQueueTask.taskId);

                removedFiles.add(mainQueueTask.file);
                removedByteSize += mainQueueTask.allocatedSize;
                mainQueueTask.file.queued = false;

                if (mainQueueTask.badFile) {
                    badFileInMainQueue.remove(root);
                }
            }

            usedQueueMemory.addAndGet(-removedByteSize);
            usedTotalMemory.addAndGet(-removedByteSize);

            return removedFiles;
        }
    }

    // guarded by "lock"
    private @Nullable FileTask findByRootInMainQueue(TSRoot root) {
        for (FileTask task : taskQueue.values()) {
            if (getRoot(task.file) == root) {
                return task;
            }
        }
        return null;
    }

    /**
     * Removes the task for the given file from the provided queue.
     *
     * @param file   The TSFile to be removed.
     * @param queue  The queue from which to remove the task.
     * @return       The removed FileTask if found; otherwise, null.
     */
    @Nullable
    private static FileTask removeFromQueue(TSFile file, TreeMap<Long, FileTask> queue) {
        FileTask foundTask = null;
        for (FileTask task : queue.values()) {
            if (task.file == file) {
                foundTask = task;
                break;
            }
        }
        if (foundTask != null) {
            queue.remove(foundTask.taskId);
        }
        return foundTask;
    }

    /**
     * Returns the next file to be processed.
     * Must be followed by {@link #reportTaskCompleted(FileTask)} call after processing completed.
     */
    @NotNull
    FileTask pollTask() throws InterruptedException {
        synchronized (lock) {
            Map.Entry<Long, FileTask> nextEntry;
            boolean wasIdle = false;
            try {
                while ((nextEntry = taskQueue.pollFirstEntry()) == null) {
                    if (!wasIdle) {
                        idleWorkers.incrementAndGet();
                        wasIdle = true;
                    }
                    lock.wait();
                }
            } finally {
                if (wasIdle) {
                    idleWorkers.decrementAndGet();
                }
            }

            FileTask task = nextEntry.getValue();
            if (task.badFile) {
                // At this point can't stop processing of this bad file, even if a good file for the same root was added later.
                // So we just remove the root from badFileInMainQueue.
                badFileInMainQueue.remove(getRoot(task.file));
            }
            usedQueueMemory.addAndGet(-task.allocatedSize);
            return task;
        }
    }

    /**
     * Allows next file for the same root to be scheduled.
     */
    void reportTaskCompleted(FileTask task) {
        TSRoot root = getRoot(task.file);
        synchronized (lock) {
            moveNextTaskFromBacklog(root);
        }
        usedTotalMemory.addAndGet(-task.allocatedSize);
    }

    // guarded by "lock"
    private void moveNextTaskFromBacklog(TSRoot root) {
        var queueForRoot = queueByRoot.get(root);
        if (queueForRoot != null) {
            var nextEntry = queueForRoot.pollFirstEntry();
            if (nextEntry != null) {
                FileTask nextTaskForRoot = nextEntry.getValue();

                // Schedule next task for this root
                taskQueue.put(nextTaskForRoot.taskId, nextTaskForRoot);
                if (nextTaskForRoot.badFile) {
                    badFileInMainQueue.add(root);
                }

                // This notify is not really necessary as we normally expect this thread to immediately poll for the next task.
                // However, we should still keep it for the case when current thread stops processing tasks for some reason (like unexpected major error).
                lock.notify();
            } else {
                // No more tasks for this root
                queueByRoot.remove(root);
                queuePool.release(queueForRoot);
            }
        } else {
            assert false : "No queue for root " + root;
        }
    }

    /**
     * @return memory used by files in the queue (including files being currently processed)
     */
    public long getUsedTotalMemory() {
        return usedTotalMemory.get();
    }

    /**
     * @return memory used by files in the queue (excluding files being currently processed)
     */
    public long getUsedQueueMemory() {
        return usedQueueMemory.get();
    }

    public AtomicInteger getIdleWorkers() {
        return idleWorkers;
    }

    // This method is slow. Should be used for assertions only.
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean contains(TSFile file) {
        synchronized (lock) {
            TSRoot root = getRoot(file);
            var queueForRoot = queueByRoot.get(root);
            if (queueForRoot == null) {
                return false;
            }
            for (FileTask task : queueForRoot.values()) {
                if (task.file == file) {
                    return true;
                }
            }
            for (FileTask task : taskQueue.values()) {
                if (task.file == file) {
                    return true;
                }
            }
            return false;
        }
    }

    private static TSRoot getRoot(TSFile tsf) {
        return tsf.getRoot();
    }

    static class FileTask {
        final long taskId;
        final TSFile file;
        final long allocatedSize;
        final boolean badFile;

        FileTask(long taskId, TSFile file, long allocatedSize, boolean badFile) {
            this.taskId = taskId;
            this.file = file;
            this.allocatedSize = allocatedSize;
            this.badFile = badFile;
        }
    }
}
