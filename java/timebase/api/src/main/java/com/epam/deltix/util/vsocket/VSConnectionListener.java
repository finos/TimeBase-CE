package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.concurrent.QuickExecutor;

import java.io.IOException;

/**
 *
 */
public interface VSConnectionListener {
    public void         connectionAccepted (
        QuickExecutor       executor,
        VSChannel           serverChannel
    ) throws IOException;
}
