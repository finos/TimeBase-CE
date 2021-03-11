package com.epam.deltix.qsrv.hf.tickdb.pub;

public class ReaderClosedException extends CursorException {

    public ReaderClosedException(String message) {
        super(message);
    }

    public ReaderClosedException(String message, Throwable cause) {
        super(message, cause);
    }
}
