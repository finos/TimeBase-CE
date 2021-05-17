/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.time.TimeKeeper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes specified task with fixed rate (of virtual time).
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class VirtualTimePeriodicTaskExecutor implements Disposable {
    private int virtualTimeSliceDurationMs;
    private Long stopAtVirtualTimestamp;
    private final EventListener eventListener;
    private double speed;
    private boolean paused = false;

    private final ManualClock virtualClock;

    // Task execution
    private ScheduledFuture<?> scheduledTask = null;
    private final VirtualClockTask periodicTask;

    /**
     * Contains two values: version of task state and current state that is encoded with last bit.
     * Even value: not running, odd value: running.
     */
    private final AtomicInteger periodicTaskStateVersion = new AtomicInteger();

    private volatile boolean stopTask = false; // Signal for running task that is should gracefully stop.
    private boolean active = false;
    // TODO: Use better scheduler?
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * @param speed initial clocks speed. 1 means normal speed.
     * @param virtualTimeSliceDurationMs virtual time interval for task execution
     * @param periodicTask task to execute
     * @param stopAtVirtualTimestamp after reaching this virtual timestamp execution will stop
     */
    public VirtualTimePeriodicTaskExecutor(double speed, int virtualTimeSliceDurationMs, VirtualClockTask periodicTask, @Nullable Long stopAtVirtualTimestamp, @Nullable EventListener eventListener) {
        if (speed < 0) {
            throw new IllegalArgumentException("Negative speed");
        }
        if (virtualTimeSliceDurationMs < 1) {
            throw new IllegalArgumentException();
        }
        this.speed = speed;
        this.virtualTimeSliceDurationMs = virtualTimeSliceDurationMs;
        this.periodicTask = periodicTask;
        this.stopAtVirtualTimestamp = stopAtVirtualTimestamp;
        this.eventListener = eventListener;
        this.virtualClock = new ManualClock();
    }

    /**
     * Starts (or-restarts) virtual clock from specified virtual time.
     *
     * @param startingVirtualTimestamp virtual time to start from
     */
    public synchronized void startFromTimestamp(long startingVirtualTimestamp) {
        stopExistingTask();

        active = true;
        virtualClock.setCurrentTimeMillis(startingVirtualTimestamp);

        schedulePeriodicTask();
    }

    public synchronized void setSpeed(double speed) {
        if (speed < 0) {
            throw new IllegalArgumentException("Negative speed");
        }

        stopExistingTask();
        this.speed = speed;
        schedulePeriodicTask();
    }

    public void setStopAtVirtualTimestamp(@Nullable Long stopAtVirtualTimestamp) {
        // We don't synchronize because we intend to change this field only on pause/resume mode.
        this.stopAtVirtualTimestamp = stopAtVirtualTimestamp;
    }

    public synchronized void setTimeSliceDuration(int durationMs) {
        if (durationMs < 1) {
            throw new IllegalArgumentException();
        }

        stopExistingTask();
        this.virtualTimeSliceDurationMs = durationMs;
        schedulePeriodicTask();
    }

    public synchronized void pause() {
        if (isPaused()) {
            return;
        }
        stopExistingTask();
        paused = true;
    }

    public synchronized void resume() {
        if (!isPaused()) {
            return;
        }
        paused = false;
        schedulePeriodicTask();
    }

    private void triggerPausedOnEndTimeEvent() {
        if (eventListener != null) {
            eventListener.pausedOnEndTime();
        }
    }

    /**
     * Just alias for pause.
     */
    public synchronized void stop() {
        pause();
    }

    @Override
    public void close() {
        stop();
        scheduler.shutdown();
    }

    private boolean isTimeRunning() {
        return !paused && speed > 0;
    }

    private boolean isPaused() {
        return paused;
    }

    private void schedulePeriodicTask() {
        if (!active || !isTimeRunning()) {
            // We should not run anything now
            return;
        }

        int realTimePeriod = (int) (virtualTimeSliceDurationMs / speed); // TODO: Should we allow lesser intervals and do calculations in nanos?
        if (realTimePeriod == 0) {
            throw new IllegalStateException("Real time period is less than 1ms. Not supported.");
        }
        assert periodicTaskStateVersion.get() % 2 == 0;
        assert scheduledTask == null;
        int startVersion = periodicTaskStateVersion.get();
        this.scheduledTask = scheduler.scheduleAtFixedRate(new PeriodicTask(startVersion), 0, realTimePeriod, TimeUnit.MILLISECONDS);
    }

    private boolean stopExistingTask() {
        if (scheduledTask == null) {
            // No existing task
            return false;
        }

        scheduledTask.cancel(false);
        stopTask = true;
        scheduledTask = null;

        // Await current task completion

        int currentVersion = periodicTaskStateVersion.get();
        int oldVersion = currentVersion & ~1; // Clear last bit
        int nextVersion = oldVersion + 2;
        while (!periodicTaskStateVersion.compareAndSet(oldVersion, nextVersion)) {
            Thread.yield();
        }
        stopTask = false;
        return true;
    }

    long getCurrentVirtualTime() {
        return virtualClock.currentTimeMillis();
    }

    private class PeriodicTask implements Runnable {
        private final int startVersion;

        PeriodicTask(int startVersion) {
            this.startVersion = startVersion;
        }

        @SuppressFBWarnings(value = "SA_FIELD_SELF_COMPUTATION", justification = "Spotbugs can't see that TimeKeeper.currentTime is volatile")
        @Override
        public void run() {
            //System.out.println("Tick");
            // Try to set "working" flag
            if (!periodicTaskStateVersion.compareAndSet(startVersion, startVersion + 1)) {
                // Version changed => this task will be stopped
                return;
            }
            boolean endTimeReached = false;
            try {
                long executionStartSystemTime = TimeKeeper.currentTime;
                boolean shouldDoNextExecution;
                do {
                    long currentVirtualTime = virtualClock.currentTimeMillis();
                    periodicTask.run(currentVirtualTime);

                    if (stopAtVirtualTimestamp != null) {
                        if (currentVirtualTime == stopAtVirtualTimestamp) {
                            // We reached end. Stop/pause.
                            endTimeReached = true;
                            break;
                        } else {
                            if (currentVirtualTime + virtualTimeSliceDurationMs > stopAtVirtualTimestamp) {
                                // New time is beyond time when we supposed to stop. Adjust clock for final invocation.
                                virtualClock.setCurrentTimeMillis(stopAtVirtualTimestamp);
                            } else {
                                virtualClock.advanceByMillis(virtualTimeSliceDurationMs);
                            }
                        }
                    } else {
                        virtualClock.advanceByMillis(virtualTimeSliceDurationMs);
                    }

                    long executionEndSystemTime = TimeKeeper.currentTime;

                    long timePassed = executionEndSystemTime - executionStartSystemTime;
                    shouldDoNextExecution = timePassed * speed > virtualTimeSliceDurationMs + 1;
                    executionStartSystemTime = executionEndSystemTime;
                } while (shouldDoNextExecution && !stopTask);
            } catch (Exception e) {
                // TODO: Decide on exception handling.
                // Log it?
                System.err.println("Got exception in periodic task: " + e);
            } finally {
                // Mark that we finished (clear "working" flag)
                periodicTaskStateVersion.set(startVersion);
            }
            if (endTimeReached) {
                synchronized (VirtualTimePeriodicTaskExecutor.this) {
                    pause();
                    triggerPausedOnEndTimeEvent();
                }
            }
        }
    }

    public interface EventListener {
        void pausedOnEndTime();
    }
}
