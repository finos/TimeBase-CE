package com.epam.deltix.qsrv.hf.tickdb.pub;

public class WriterAbortedException extends LoadingError {
    public WriterAbortedException() {
    }

    public WriterAbortedException(String message) {
        super(message);
    }

    public WriterAbortedException(String message, Throwable cause) {
        super(message, cause);
    }

    public WriterAbortedException(Throwable cause) {
        super(cause);
    }
}
