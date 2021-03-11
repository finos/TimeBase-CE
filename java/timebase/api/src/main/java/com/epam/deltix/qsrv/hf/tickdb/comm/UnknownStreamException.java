package com.epam.deltix.qsrv.hf.tickdb.comm;

public class UnknownStreamException extends RuntimeException {

    public UnknownStreamException() {
    }

    public UnknownStreamException(String message) {
        super(message);
    }

    public UnknownStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownStreamException(Throwable cause) {
        super(cause);
    }
}