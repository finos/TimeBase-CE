package com.epam.deltix.util.oauth.impl;

import java.util.Timer;
import java.util.TimerTask;

public class TimerTokenScheduler implements RefreshTokenScheduler {

    private final Timer timer;

    public TimerTokenScheduler(Timer timer) {
        this.timer = timer;
    }

    @Override
    public void schedule(long timestampMs, Runnable task) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, timestampMs);
    }

    @Override
    public void close() {
    }
}
