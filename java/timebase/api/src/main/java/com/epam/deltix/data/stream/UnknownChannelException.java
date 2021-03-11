package com.epam.deltix.data.stream;

public class UnknownChannelException extends RuntimeException {

    public UnknownChannelException() {
    }

    public UnknownChannelException(String message) {
        super(message);
    }

    public UnknownChannelException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnknownChannelException(Throwable cause) {
        super(cause);
    }
}
