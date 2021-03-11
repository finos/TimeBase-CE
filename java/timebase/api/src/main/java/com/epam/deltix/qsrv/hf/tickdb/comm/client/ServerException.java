package com.epam.deltix.qsrv.hf.tickdb.comm.client;

public class ServerException extends RuntimeException {
    
    public ServerException() {
    }

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServerException(Throwable cause) {
        super(cause);
    }
}
