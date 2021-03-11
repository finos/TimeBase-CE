package com.epam.deltix.qsrv.hf.tickdb.replication;

/**
 *
 */
public class ReplicationException extends RuntimeException {

    public ReplicationException(String message) {
        super(message);
    }

    public ReplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
