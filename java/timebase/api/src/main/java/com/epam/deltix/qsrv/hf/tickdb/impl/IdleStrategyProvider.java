package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
public final class IdleStrategyProvider {
    public static IdleStrategy getIdleStrategy(ChannelPerformance channelPerformance) {
        switch (channelPerformance) {
            case MIN_CPU_USAGE:
            default:
                return new BackoffIdleStrategy(1, 1, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(1000));

            case LOW_LATENCY:
                // Yield a bit and switch to short waits
                return new BackoffIdleStrategy(1, 100, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(10));

            case LATENCY_CRITICAL:
                // For Java 9 it's better to spin with Thread.onSpinWait() (see http://openjdk.java.net/jeps/285)
                return new BusySpinIdleStrategy();
        }
    }
}
