package com.epam.deltix.qsrv.hf.tickdb.pub;

public class WriterClosedException extends LoadingError {
    public WriterClosedException() {
    }

    public WriterClosedException(String message) {
        super(message);
    }

    public WriterClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public WriterClosedException(Throwable cause) {
        super(cause);
    }
}
