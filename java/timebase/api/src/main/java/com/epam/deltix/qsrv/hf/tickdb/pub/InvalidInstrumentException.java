package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *
 */
public class InvalidInstrumentException extends LoadingError {

    public InvalidInstrumentException(String message) {
        super(message);
    }

    public InvalidInstrumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidInstrumentException(Throwable cause) {
        super(cause);
    }
}
