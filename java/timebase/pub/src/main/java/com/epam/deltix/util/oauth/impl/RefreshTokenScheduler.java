package com.epam.deltix.util.oauth.impl;

import com.epam.deltix.util.lang.Disposable;

public interface RefreshTokenScheduler extends Disposable {

    void schedule(long delayMs, Runnable task);

}
