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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaConverter;
import com.epam.deltix.util.time.TimeKeeper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
* Replays messages from specific streams in real-time.
*/
@ParametersAreNonnullByDefault
public class RealtimePlayerThread extends Thread {

    public enum PlayMode {
        STOP,
        PAUSED,
        PLAY,
        SKIP
    }

    private volatile PlayMode playMode = PlayMode.PLAY;
    private final MessageSource<InstrumentMessage> src;
    private final MessageChannel<InstrumentMessage> dest;
    private final SchemaConverter converter;
    private final Runnable streamRestarter;
    private final Lock lock = new ReentrantLock();
    private final Condition commonCondition = lock.newCondition();
    private final Condition pauseBreakCondition = lock.newCondition();
    private double oldSpeed; // to recalculate the waiting time
    private volatile double speed;
    private volatile boolean updateTimestampPoint = false;
    private long messageTimestampPoint = Long.MIN_VALUE; // timestamp point of the message source
    private long realTimestampPoint = Long.MIN_VALUE; // real time timestamp point
    private volatile long endTimeNano = Long.MAX_VALUE;
    protected long count = 1;

    /**
     * @param streamRestarter will be executed (if not null) when source stream depletes (ends) to restart (cycle) it
     * @param speed ratio of time periods between messages in src and dest
     */
    public RealtimePlayerThread(
            MessageSource<InstrumentMessage> src,
            MessageChannel<InstrumentMessage> dest,
            SchemaConverter converter,
            @Nullable
            Runnable streamRestarter,
            double speed) {
        super("Player Thread");

        this.src = src;
        this.dest = dest;
        this.converter = converter;
        this.streamRestarter = streamRestarter;
        oldSpeed = this.speed = speed;

        this.setDaemon(false);
    }

    public void setMode(PlayMode mode) {
        lock.lock();
        try {
            if (mode != playMode){
                if (mode == PlayMode.SKIP || mode == PlayMode.PAUSED)
                    updateTimestampPoint = true;
                playMode = mode;
                commonCondition.signal();
                if (mode == PlayMode.PLAY || mode == PlayMode.SKIP || mode == PlayMode.STOP)
                    pauseBreakCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    public void setEndTimeNano(long endTimeNano) {
        this.endTimeNano = endTimeNano;
    }

    public void setSpeed(double speed) {
        if (speed <= 0) {
            throw new IllegalArgumentException("Playback speed cannot be zero or less than zero");
        }
        lock.lock();
        try {
            if (this.speed != speed) {
                updateTimestampPoint = true;
                this.speed = speed;
                commonCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void run() {
        boolean first = true;
        long lastMessageTimestamp = Long.MIN_VALUE;
        long lastRealTimestamp = Long.MIN_VALUE;

        while (true) {
            long now;
            long messageTimestampNs;
            RawMessage outMsg;

            lock.lock();
            try {
                if (playMode == PlayMode.STOP) {
                    return;
                }
                if (updateTimestampPoint) {
                    updateTimestampPoint(lastMessageTimestamp, lastRealTimestamp);
                }

                if (!src.next()) {
                    if (recycle()) {
                        first = true;
                    } else {
                        playMode = PlayMode.STOP;
                        return;
                    }
                }

                RawMessage inMsg = (RawMessage) src.getMessage();

                messageTimestampNs = inMsg.getNanoTime();
                if (messageTimestampNs >= endTimeNano) {
                    if (recycle()) {
                        first = true;
                        inMsg = (RawMessage) src.getMessage();
                        messageTimestampNs = inMsg.getNanoTime();
                    } else {
                        playMode = PlayMode.STOP;
                        return;
                    }
                }
                now = TimeKeeper.currentTimeNanos;

                if (first) {
                    first = false;
                    messageTimestampPoint = messageTimestampNs;
                    realTimestampPoint = now;
                } else {
                    if (playMode == PlayMode.SKIP)
                        playMode = PlayMode.PLAY;
                    // Higher speed means less time to wait
                    long waitTime = getWaitTime(messageTimestampNs, now);
                    try {
                        if (playMode == PlayMode.PAUSED) {
                            pauseBreakCondition.await();
                            waitTime = oldSpeed == speed ? waitTime : recalculateWaitingTime(waitTime);
                            if (playMode == PlayMode.STOP || playMode == PlayMode.SKIP) {
                                waitTime = 0;
                            }
                        }
                        while (waitTime > 2000000) {
                            long waitTimeLeft = commonCondition.awaitNanos(waitTime);
                            if (waitTimeLeft > 0 && waitTimeLeft <= waitTime) {
                                if (oldSpeed != speed) {
                                    waitTime = recalculateWaitingTime(waitTimeLeft);
                                    continue;
                                }
                                if (playMode == PlayMode.PAUSED) {
                                    pauseBreakCondition.await();
                                    waitTime = oldSpeed == speed ? waitTimeLeft : recalculateWaitingTime(waitTimeLeft);
                                    if (playMode == PlayMode.PLAY) {
                                        continue;
                                    }
                                }
                            }
                            break; // break because the waiting time is up or received a signal on "next"
                        }
                    } catch (InterruptedException e) {
                        if (Thread.currentThread().isInterrupted()) {
                            playMode = PlayMode.STOP;
                        }
                    }
                }

                outMsg = converter.convert(inMsg);
                if (outMsg == null) {
                    onMessageConversionError(inMsg);
                    continue;
                }

                now = TimeKeeper.currentTimeNanos;
                outMsg.setNanoTime(now);
                dest.send(outMsg);

                if (updateTimestampPoint) {
                    updateTimestampPoint(messageTimestampNs, now);
                }
            } finally {
                lock.unlock();
            }

            if (playMode == PlayMode.STOP)
                return;

            lastMessageTimestamp = messageTimestampNs;
            lastRealTimestamp = now;
            log(messageTimestampNs, now, outMsg);
            count++;
        }
    }

    protected void log(long mt, long now, RawMessage outMsg) {
        // do nothing by default
    }

    protected void onMessageConversionError (RawMessage msg) {
        System.err.println("Cannot convert message:" + msg);
    }

    private boolean recycle(){
        // Source depleted
        if (streamRestarter == null) {
            return false;
        } else {
            // Cyclic mode: restart stream
            streamRestarter.run();
            // Even after reset we don't have any messages. So stream is empty. Exit.
            return src.next();
        }
    }

    private long getWaitTime(long mt, long now) {
        // calculate the wait time relative to realTimestampPoint to adjust for pauses and speed changes
        long sourceTimePassed = mt - messageTimestampPoint;
        long expectedRealTimePassed = speed == 1 ? sourceTimePassed : Math.round(sourceTimePassed / speed);
        long expectedRealTimestamp = addWithOverflowCheck(realTimestampPoint, expectedRealTimePassed);
        return expectedRealTimestamp - now;
    }

    private void updateTimestampPoint(long messageTimestampPoint, long realTimestampPoint) {
        updateTimestampPoint = false;
        this.messageTimestampPoint = messageTimestampPoint;
        this.realTimestampPoint = realTimestampPoint;
        resetSpeedDiff();
    }


    private long recalculateWaitingTime(long waitTime) {
        long result = Math.round(waitTime * oldSpeed / speed);
        resetSpeedDiff();
        return result;
    }

    private void resetSpeedDiff() {
        oldSpeed = speed;
    }

    private long addWithOverflowCheck(long x, long y) {
        long r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) {
            throw new ArithmeticException("Failed to determine the value of the next realtime message, intervals between messages are too long");
        }
        return r;
    }
}