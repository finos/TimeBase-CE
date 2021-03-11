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
    private static volatile ChannelExecutor INSTANCE;

    private static ChannelExecutor createInstance(AffinityConfig affinityConfig) {
        ChannelExecutor executor = create(affinityConfig);
        executor.thread.start();
        return executor;
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
    private final CPUEater          cpuEater;
    private final int               idleTime;
    private final Thread thread;

    private static ChannelExecutor create(AffinityConfig affinityConfig) {
        ThreadFactory factory = new AffinityThreadFactoryBuilder(affinityConfig)
                .setNameFormat("ChannelExecutor Thread")
                .setDaemon(true)
                .build();

        return new ChannelExecutor(factory);
    }

    private ChannelExecutor(ThreadFactory factory) {
        idleTime = VSProtocol.getIdleTime();
        cpuEater = new CPUEater(idleTime);

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
        synchronized (channels) {
            channels.linkLast(new Entry(channel));
        }

        wakeup();
    }

    @Override
    public void run() {
        assert Thread.currentThread() == this.thread;

        while (!stopped) {
            Entry entry;

            synchronized (channels) {
                entry = channels.getFirst();
            }

            if (entry == null) {
                LockSupport.park();

                if (Thread.interrupted ()) {
                    if (stopped)
                        break;
                }
            }

            synchronized (channels) {
                entry = channels.getFirst();
                while (entry != null) {

                    VSChannel channel = entry.channel;
                    try {
                        if (channel != null && channel.getNoDelay() && channel.getState() == VSChannelState.Connected) {
                            VSOutputStream out = channel.getOutputStream();
                            out.flushAvailable();

                            entry = entry.next();
                        } else if (channel != null) {
                            if (channel.getState() == VSChannelState.Removed || channel.getState() == VSChannelState.Closed)
                                entry = remove(entry);
                        }
                    } catch (ChannelClosedException e) {
                        // ignore
                        entry = remove(entry);
                    } catch (IOException e) {
                        VSProtocol.LOGGER.log (Level.WARNING, "Exception while flushing data", e);
                    }
                }
            }

            if (!Util.IS_WINDOWS_OS) {
                LockSupport.parkNanos (idleTime);
            } else {
                if (TimeKeeper.getMode() == TimeKeeper.Mode.HIGH_RESOLUTION_SYNC_BACK)
                    TimeKeeper.parkNanos(idleTime);
                else
                    cpuEater.run();
            }
        }
    }

    private Entry        remove(Entry entry) {
        Entry next = entry.next();
        entry.unlink();
        return next;
    }

    private static class Entry extends QuickList.Entry<Entry> {
        VSChannel channel;

        private Entry(VSChannel channel) {
            this.channel = channel;
        }
    }

    private static class CPUEater {
        private final long  avgCostOfNanoTimeCall;
        private final long  cycles;

        private final MemoryDataOutput out = new MemoryDataOutput();
        private final double value = 345.56787899;

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