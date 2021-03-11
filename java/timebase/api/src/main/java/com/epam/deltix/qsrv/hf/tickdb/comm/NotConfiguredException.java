package com.epam.deltix.qsrv.hf.tickdb.comm;

/**
 * Signals that I/O error occurs while opening timebase connection.
 */
public class NotConfiguredException extends RuntimeException {
    public NotConfiguredException() {
    }

    public NotConfiguredException(String message) {
        super(message);
    }

    public NotConfiguredException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotConfiguredException(Throwable cause) {
        super(cause);
    }
}
