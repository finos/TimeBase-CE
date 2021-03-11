package com.epam.deltix.qsrv.hf.tickdb.pub.lock;

import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingError;

/**
 * Signals that stream has been locked and current operation on the stream cannot be executed.
 */
public class StreamLockedException extends LoadingError {
    
    public StreamLockedException() {
    }

    public StreamLockedException(String message) {
        super(message);
    }

    public StreamLockedException(String message, Throwable cause) {
        super(message, cause);
    }

    public StreamLockedException(Throwable cause) {
        super(cause);
    }
}
