package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 * Recoverable exceptions. Marks all cursor exceptions that do not invalidate cursor (i.e. client can retry reading attempt).
 */
public abstract class CursorException extends RuntimeException {
    public CursorException() {
    }

    public CursorException(String message) {
        super (message);
    }

    public CursorException(String message, Throwable cause) {
        super(message, cause);
    }
}
