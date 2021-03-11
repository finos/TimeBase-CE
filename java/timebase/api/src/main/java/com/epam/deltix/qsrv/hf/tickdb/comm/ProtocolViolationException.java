package com.epam.deltix.qsrv.hf.tickdb.comm;

/**
 *
 */
public class ProtocolViolationException extends RuntimeException {
    public ProtocolViolationException (String message) {
        super (message);
    }
}
