package com.epam.deltix.qsrv.hf.pub;

public class UnknownDestinationException extends RuntimeException {
    public UnknownDestinationException(String message) {
        super(message);
    }

    public UnknownDestinationException(String message, Throwable cause) {
        super(message, cause);
    }
}
