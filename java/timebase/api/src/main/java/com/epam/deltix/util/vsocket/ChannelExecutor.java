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
package com.epam.deltix.util.vsocket;

import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.thread.affinity.AffinityThreadFactoryBuilder;
import com.epam.deltix.util.collections.QuickList;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.TimeKeeper;

import java.io.IOException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;

class ChannelExecutor implements Runnable {
    //@ApiStatus.Experimental // Temporary option for testing performance effect of using yield on Windows
    private static final boolean USE_YIELD_ON_WINDOWS = Boolean.getBoolean("TimeBase.network.executor.windows.useYield");

    private static volatile ChannelExecutor INSTANCE;

    private static ChannelExecutor createInstance(AffinityConfig affinityConfig) {
        ChannelExecutor executor = create(affinityConfig);
        executor.thread.start();
        return executor;
    }

    @SuppressWarnings("SameParameterValue")
    //@VisibleForTesting
    static ChannelExecutor createNonSharedTestInstance(AffinityConfig affinityConfig) {
        return createInstance(affinityConfig);
    }

    public static ChannelExecutor getInstance(AffinityConfig affinityConfig) {
        // "Double checked lock" (via volatile)
        if (INSTANCE == null) {
            synchronized (ChannelExecutor.class) {
                if (INSTANCE == null) {
                    INSTANCE = createInstance(affinityConfig);
                }
            }
        }
        return INSTANCE;
    }

    private final QuickList<Entry>  channels = new QuickList<>();
    private boolean                 stopped = false;
    private final CPUEater          cpuEater; // Used only for Windows
    private final int               idleTime;
    private final Thread thread;

    static ChannelExecutor create(AffinityConfig affinityConfig) {
        ThreadFactory factory = new AffinityThreadFactoryBuilder(affinityConfig)
                .setNameFormat("ChannelExecutor Thread")
                .setDaemon(true)
                .build();

        return new ChannelExecutor(factory);
    }

    private ChannelExecutor(ThreadFactory factory) {
        idleTime = VSProtocol.getIdleTime();
        cpuEater = Util.IS_WINDOWS_OS ? new CPUEater(idleTime) : null;

        this.thread = factory.newThread(this);
    }

    public void                wakeup() {
        LockSupport.unpark(this.thread);
    }

    public void                 shutdown () {
        stopped = true;
        wakeup ();
    }

    public void                 addChannel(VSChannel channel) {
        assert channel != null;

        if (channel == null)
            return;

        synchronized (channels) {
            channels.linkLast(new Entry(channel));
        }

        wakeup();
    }

    public void                 removeChannel(VSChannel channel) {
        assert channel != null;


        synchronized (channels) {
            Entry entry = channels.getFirst();

            while (entry != null) {
                if (entry.channel.equals(channel)) {
                    remove(entry);
                    return;
                } else {
                    entry = entry.next();
                }
            }
        }

        wakeup();
    }

    @Override
    public void run() {
        assert Thread.currentThread() == this.thread;

        while (!stopped) {
            Entry entry;
            boolean isEmpty;

            long bytesSent = 0;
            synchronized (channels) {
                entry = channels.getFirst();
                isEmpty = entry == null;

                while (entry != null) {
                    VSChannel channel = entry.channel;
                    try {
                        switch (channel.getState()) {
                            case Connected: {
                                if (channel.getNoDelay()) {
                                    // Flush
                                    VSOutputStream out = channel.getOutputStream();
                                    // We do not want to send all at once, we will, re-try send shortly
                                    bytesSent += out.flushAvailable(false);
                                }
                                break;
                            }
                            case Removed:
                            case Closed: {
                                entry = remove(entry);
                                continue;
                            }
                        }
                    } catch (ChannelClosedException e) {
                        // ignore
                        entry = remove(entry);
                        continue;
                    } catch (IOException e) {
                        VSProtocol.LOGGER.log (Level.WARNING, "Exception while flushing data", e);
                    }

                    // Move to the next channel
                    entry = entry.next();
                }
            }

            if (isEmpty) {
                // No channels to process => Wait for channels to be added.
                LockSupport.park();

                if (Thread.interrupted ()) {
                    if (stopped) {
                        break;
                    }
                }
            } else {
                // Do not wait if we have sent any data.
                // It's very likely that it's time to send more because the sending time is relatively long.
                if (bytesSent == 0) {
                    // Wait till next time to flush channels
                    idleWait();
                }
            }
        }
    }

    private void idleWait() {
        if (!Util.IS_WINDOWS_OS) {
            if (USE_YIELD_ON_WINDOWS) {
                waitWithYield(idleTime);
            } else {
                LockSupport.parkNanos(idleTime);
            }
        } else {
            if (TimeKeeper.getMode() == TimeKeeper.Mode.HIGH_RESOLUTION_SYNC_BACK) {
                TimeKeeper.parkNanos(idleTime);
            } else {
                cpuEater.run();
            }
        }
    }

    private static void waitWithYield(int idleTime) {
        long start = System.nanoTime();
        long end = start + idleTime;
        while (System.nanoTime() < end) {
            Thread.yield();
        }
    }

    private Entry        remove(Entry entry) {
        Entry next = entry.next();
        entry.unlink();
        return next;
    }

    private static class Entry extends QuickList.Entry<Entry> {
        final VSChannel channel;

        private Entry(VSChannel channel) {
            this.channel = channel;
        }
    }

    private static class CPUEater {
        private final long  avgCostOfNanoTimeCall;
        private final long  cycles;

        private final MemoryDataOutput out = new MemoryDataOutput();
        private static final double value = 345.56787899;

        private CPUEater(long nanos) {
            this.avgCostOfNanoTimeCall = nanoTimeCost();

            if (nanos <= avgCostOfNanoTimeCall)
                throw new IllegalArgumentException("Input time is too small: " + nanos);

            // warmup
            for (int j = 0; j < 1000; j++)
                execute(100);

            long time10 = measureExecution(10);
            long time50 = measureExecution(50);
            double c = time50 / time10 / 5.0;

            long low = (nanos / time10 * 10);
            long high = (long) (low / c);
            long count = low + (high - low) / 2;
            long increment = Math.abs((high - low) / 4);

            if (increment == 0)
                increment = 100;

            long time = measureExecution(count);
            while (time < nanos) {
                count += increment;
                time = measureExecution(count);
            }
            cycles = low;
        }

        private static long     nanoTimeCost() {
            final int N = 30000;
            long enterTime = System.nanoTime();
            for (int i = 0; i < N; i++) {
                System.nanoTime();
            }
            long exitTime = System.nanoTime();
            return (exitTime - enterTime) / (N + 2);
        }

        private void            execute(long cycles) {
            for (int i = 0; i < cycles; i++) {
                out.reset();
                out.writeScaledDouble(value);
            }
        }

//        // non-deterministic execution time on high cpu load
//        private void            execute(long cycles) {
//            for (int i = 0; i < cycles; i++) {
//                try {
//                    Thread.sleep(0);
//                } catch (InterruptedException e) {
//                    // ignore
//                }
//            }
//        }

        public void             run() {
            execute(cycles);
        }

        private long            measureExecution(long cycles) {
            long enterTime = System.nanoTime();
            for (int j = 0; j < 20000; j++)
                execute(cycles);
            long exitTime = System.nanoTime();
            long time = avgCostOfNanoTimeCall + (exitTime - enterTime) / 20000;
            //System.out.println("Time of execution(" + cycles  + "): " + time);
            return time;
        }
    }
}