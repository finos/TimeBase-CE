package com.epam.deltix.qsrv.hf.tickdb.pub;

public class LoadingError extends RuntimeException {
    public LoadingError() {
    }

    public LoadingError(String message) {
        super(message);
    }

    public LoadingError(String message, Throwable cause) {
        super(message, cause);
    }

    public LoadingError(Throwable cause) {
        super(cause);
    }
}
