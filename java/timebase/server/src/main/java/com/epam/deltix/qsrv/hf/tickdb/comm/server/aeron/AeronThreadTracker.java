package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.epam.deltix.qsrv.hf.tickdb.pub.InsufficientCpuResourcesException;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

/**
 * Tracks "busy spin" threads that we create to serve Aeron-based cursor and loaders
 *
 * @author Alexei Osipov
 */
public class AeronThreadTracker {
    private static int MAX_LATENCY_CRITICAL_THREADS = Integer.getInteger("TimeBase.requestHandler.maxLatencyCriticalThreads", Runtime.getRuntime().availableProcessors() / 2); // Note: may be zero

    // These thread factories must be static to keep JVM unique thread names
    private static ThreadFactory aeronUploaderThreadFactory = new ThreadFactoryBuilder().setNameFormat("Aeron-Uploader-%d").build();
    private static ThreadFactory aeronDownloaderThreadFactory = new ThreadFactoryBuilder().setNameFormat("Aeron-Downloader-%d").build();
    private static ThreadFactory aeronMulticastDownloaderThreadFactory = new ThreadFactoryBuilder().setNameFormat("Aeron-MDownloader-%d").build();

    // Threads that used to copy data from topics to streams
    private static ThreadFactory copyTopicToStreamThreadFactory = new ThreadFactoryBuilder().setNameFormat("Copy-Topic-to-Stream-%d").build();


    private final Set<Runnable> runningTasks = new HashSet<>();

    public Thread newUploaderThread(Runnable r, boolean limit) throws InsufficientCpuResourcesException {
        return newThread(r, aeronUploaderThreadFactory, limit);
    }

    public Thread newDownloaderThread(Runnable r, boolean limit) throws InsufficientCpuResourcesException {
        return newThread(r, aeronDownloaderThreadFactory, limit);
    }

    public Thread newMulticastDownloaderThread(Runnable r, boolean limit) throws InsufficientCpuResourcesException {
        return newThread(r, aeronMulticastDownloaderThreadFactory, limit);
    }

    public Thread newCopyTopicToStreamThread(Runnable r) {
        return copyTopicToStreamThreadFactory.newThread(r);
    }

    private Thread newThread(final Runnable r, ThreadFactory threadFactory, boolean limit) throws InsufficientCpuResourcesException {
        if (limit) {
            return newThreadWithLimitCheck(r, threadFactory);
        } else {
            return threadFactory.newThread(r);
        }
    }

    private Thread newThreadWithLimitCheck(final Runnable r, ThreadFactory threadFactory) throws InsufficientCpuResourcesException {
        synchronized (runningTasks) {
            if (runningTasks.size() >= MAX_LATENCY_CRITICAL_THREADS) {
                throw new InsufficientCpuResourcesException();
            }

            if (runningTasks.contains(r)) {
                throw new IllegalStateException();
            }

            runningTasks.add(r);

            Runnable wrap = new Wrap(r, runningTasks);
            return threadFactory.newThread(wrap);
        }
    }

    private static class Wrap implements Runnable {
        private final Runnable r;
        private final Set<Runnable> runningTasks;

        private Wrap(Runnable r, Set<Runnable> runningTasks) {
            this.r = r;
            this.runningTasks = runningTasks;
        }

        @Override
        public void run() {
            try {
                r.run();
            } finally {
                synchronized (runningTasks) {
                    runningTasks.remove(r);
                }
            }
        }
    }
}
