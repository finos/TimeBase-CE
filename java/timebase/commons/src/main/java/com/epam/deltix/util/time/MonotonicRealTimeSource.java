package com.epam.deltix.util.time;

import com.epam.deltix.qsrv.hf.pub.TimeSource;
import com.epam.deltix.util.annotations.TimestampMs;
import com.epam.deltix.util.annotations.TimestampNs;
import net.jcip.annotations.ThreadSafe;

import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
public class MonotonicRealTimeSource implements TimeSource {
    private static final AtomicLong lastTimeNs = new AtomicLong(Long.MIN_VALUE);
    private static final MonotonicRealTimeSource INSTANCE = new MonotonicRealTimeSource();

    private MonotonicRealTimeSource() {
    }

    public static MonotonicRealTimeSource getInstance() {
        return INSTANCE;
    }

    @TimestampMs
    public long currentTimeMillis() {
        return this.currentTimeNanos() / 1000000L;
    }

    @TimestampNs
    public long currentTimeNanos() {
        long currentTimeNanos = RealtimeClock.INSTANCE.time();

        long prevVal;
        do {
            prevVal = lastTimeNs.get();
            if (prevVal >= currentTimeNanos) {
                return prevVal;
            }
        } while(!lastTimeNs.compareAndSet(prevVal, currentTimeNanos));

        return currentTimeNanos;
    }
}
