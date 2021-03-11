package com.epam.deltix.qsrv.hf.pub.codec;

/**
 *
 */
public class MetaDataBindException extends RuntimeException {
    public MetaDataBindException (Throwable cause) {
        super (cause);
    }

    public MetaDataBindException (String message, Throwable cause) {
        super (message, cause);
    }

    public MetaDataBindException (String message) {
        super (message);
    }
}
